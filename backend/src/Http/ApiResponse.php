<?php

declare(strict_types=1);

namespace App\Http;

use App\Entity\Owner;
use App\Model\AuthenticationResult;
use Symfony\Component\HttpFoundation\JsonResponse;

final class ApiResponse
{
    public static function json(array $data, int $status = 200): JsonResponse
    {
        return new JsonResponse($data, $status, ['Cache-Control' => 'no-store', 'Pragma' => 'no-cache']);
    }

    public static function error(int $status, string $code, string $message, array $fields = []): JsonResponse
    {
        return self::json(['error' => ['code' => $code, 'message' => $message, 'fields' => (object) $fields]], $status);
    }

    public static function ownerData(Owner $owner): array
    {
        return [
            'id' => $owner->getId()->toRfc4122(),
            'name' => $owner->getName(),
            'email' => $owner->getAccount()->getEmail(),
            'phone' => $owner->getPhone(),
        ];
    }

    public static function authentication(AuthenticationResult $result, int $status = 200): JsonResponse
    {
        return self::json([
            'token' => $result->credentials->getToken(),
            'tokenType' => 'Bearer',
            'expiresAt' => $result->credentials->session->getExpiresAt()->format(DATE_ATOM),
            'owner' => self::ownerData($result->owner),
        ], $status);
    }
}
