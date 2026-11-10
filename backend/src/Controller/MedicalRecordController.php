<?php

declare(strict_types=1);

namespace App\Controller;

use App\Entity\Account;
use App\Entity\MedicalRecord;
use App\Http\ApiResponse;
use App\Http\JsonInput;
use App\Http\MedicalRecordResponse;
use App\Model\MedicalRecordType;
use App\Service\MedicalRecordService;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\HttpKernel\Attribute\AsController;
use Symfony\Component\Routing\Attribute\Route;
use Symfony\Component\Security\Http\Attribute\CurrentUser;

#[AsController]
#[Route('/api/v1/pets/{petId}')]
final class MedicalRecordController
{
    private const COLLECTIONS = 'weight-records|vaccination-records|vet-visits';

    public function __construct(private readonly MedicalRecordService $records)
    {
    }

    #[Route('/records', name: 'api_records_history', methods: ['GET'])]
    public function history(string $petId, Request $request, #[CurrentUser] Account $account): JsonResponse
    {
        return ApiResponse::json(array_map(MedicalRecordResponse::data(...), $this->records->history($account, $petId, $request->query->all())));
    }

    #[Route('/health', name: 'api_records_health', methods: ['GET'])]
    public function health(string $petId, #[CurrentUser] Account $account): JsonResponse
    {
        return ApiResponse::json(array_map(
            static fn (?MedicalRecord $record): ?array => $record === null ? null : MedicalRecordResponse::data($record),
            $this->records->health($account, $petId),
        ));
    }

    #[Route('/{collection}', name: 'api_records_list', requirements: ['collection' => self::COLLECTIONS], methods: ['GET'])]
    public function list(string $petId, string $collection, Request $request, #[CurrentUser] Account $account): JsonResponse
    {
        return ApiResponse::json(array_map(MedicalRecordResponse::data(...), $this->records->list($account, $petId, MedicalRecordType::fromCollection($collection), $request->query->all())));
    }

    #[Route('/{collection}', name: 'api_records_create', requirements: ['collection' => self::COLLECTIONS], methods: ['POST'])]
    public function create(string $petId, string $collection, Request $request, #[CurrentUser] Account $account): JsonResponse
    {
        return ApiResponse::json(MedicalRecordResponse::data($this->records->create($account, $petId, MedicalRecordType::fromCollection($collection), JsonInput::read($request))), 201);
    }

    #[Route('/{collection}/{id}', name: 'api_records_get', requirements: ['collection' => self::COLLECTIONS], methods: ['GET'])]
    public function get(string $petId, string $collection, string $id, #[CurrentUser] Account $account): JsonResponse
    {
        return ApiResponse::json(MedicalRecordResponse::data($this->records->get($account, $petId, MedicalRecordType::fromCollection($collection), $id)));
    }

    #[Route('/{collection}/{id}', name: 'api_records_update', requirements: ['collection' => self::COLLECTIONS], methods: ['PATCH'])]
    public function update(string $petId, string $collection, string $id, Request $request, #[CurrentUser] Account $account): JsonResponse
    {
        return ApiResponse::json(MedicalRecordResponse::data($this->records->update($account, $petId, MedicalRecordType::fromCollection($collection), $id, JsonInput::read($request))));
    }

    #[Route('/{collection}/{id}', name: 'api_records_delete', requirements: ['collection' => self::COLLECTIONS], methods: ['DELETE'])]
    public function delete(string $petId, string $collection, string $id, #[CurrentUser] Account $account): Response
    {
        $this->records->delete($account, $petId, MedicalRecordType::fromCollection($collection), $id);

        return new Response(null, 204, ['Cache-Control' => 'no-store', 'Pragma' => 'no-cache']);
    }
}
