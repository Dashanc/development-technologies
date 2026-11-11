<?php

declare(strict_types=1);

namespace App\Service;

use App\Entity\Account;
use App\Exception\ApiError;
use Doctrine\DBAL\Connection;
use Symfony\Component\HttpFoundation\JsonResponse;

final class IdempotencyService
{
    public function __construct(private readonly Connection $connection)
    {
    }

    public function create(Account $account, ?string $key, string $path, array $input, callable $action): JsonResponse
    {
        if ($key === null || $key === '') {
            throw new ApiError(400, 'IDEMPOTENCY_KEY_REQUIRED', 'error.idempotency_key_required');
        }
        if (preg_match('/^[\x21-\x7e]{1,128}$/D', $key) !== 1) {
            throw new ApiError(400, 'INVALID_IDEMPOTENCY_KEY', 'error.idempotency_key_invalid');
        }
        $accountId = $account->getId()->toRfc4122();
        $keyHash = hash('sha256', $key);
        $requestHash = hash('sha256', json_encode(['POST', $path, $this->canonical((object) $input)], JSON_THROW_ON_ERROR));
        $this->connection->executeStatement('DELETE FROM api_idempotency WHERE expires_at <= CURRENT_TIMESTAMP');

        return $this->connection->transactional(function () use ($accountId, $keyHash, $requestHash, $action): JsonResponse {
            $this->connection->executeQuery('SELECT pg_advisory_xact_lock(hashtextextended(:scope, 0))', ['scope' => $accountId.':'.$keyHash])->free();
            $this->connection->executeStatement(
                'DELETE FROM api_idempotency WHERE account_id = :account AND key_hash = :key AND expires_at <= CURRENT_TIMESTAMP',
                ['account' => $accountId, 'key' => $keyHash],
            );
            $cached = $this->connection->fetchAssociative(
                'SELECT request_hash, response_status, response_body, response_etag FROM api_idempotency WHERE account_id = :account AND key_hash = :key AND expires_at > CURRENT_TIMESTAMP',
                ['account' => $accountId, 'key' => $keyHash],
            );
            if ($cached !== false) {
                if (!hash_equals($cached['request_hash'], $requestHash)) {
                    throw new ApiError(409, 'IDEMPOTENCY_CONFLICT', 'error.idempotency_conflict');
                }
                $response = new JsonResponse($cached['response_body'], (int) $cached['response_status'], ['Cache-Control' => 'no-store', 'Pragma' => 'no-cache'], true);
                if ($cached['response_etag'] !== null) {
                    $response->headers->set('ETag', $cached['response_etag']);
                }

                return $response;
            }
            $response = $action();
            if (!$response instanceof JsonResponse || !$response->isSuccessful()) {
                throw new \LogicException('Only successful resource creation can be stored for idempotency.');
            }
            $this->connection->executeStatement(
                "INSERT INTO api_idempotency (account_id, key_hash, request_hash, response_status, response_body, response_etag, expires_at) VALUES (:account, :key, :request, :status, :body, :etag, CURRENT_TIMESTAMP + INTERVAL '24 hours')",
                [
                    'account' => $accountId, 'key' => $keyHash, 'request' => $requestHash,
                    'status' => $response->getStatusCode(), 'body' => $response->getContent(), 'etag' => $response->headers->get('ETag'),
                ],
            );

            return $response;
        });
    }

    private function canonical(mixed $value): mixed
    {
        if ($value instanceof \stdClass) {
            $properties = get_object_vars($value);
            ksort($properties, SORT_STRING);

            return (object) array_map($this->canonical(...), $properties);
        }
        if (is_array($value)) {
            return array_map($this->canonical(...), $value);
        }

        return $value;
    }
}
