<?php

declare(strict_types=1);

namespace App\Controller;

use App\Entity\Account;
use App\Http\ApiResponse;
use App\Http\JsonInput;
use App\Http\PetResponse;
use App\Service\IdempotencyService;
use App\Service\PetService;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\HttpKernel\Attribute\AsController;
use Symfony\Component\Routing\Attribute\Route;
use Symfony\Component\Security\Http\Attribute\CurrentUser;

#[AsController]
#[Route('/api/v1/pets')]
final class PetController
{
    public function __construct(
        private readonly PetService $pets,
        private readonly IdempotencyService $idempotency,
    ) {
    }

    #[Route('', name: 'api_pets_list', methods: ['GET'])]
    public function list(#[CurrentUser] Account $account): JsonResponse
    {
        return ApiResponse::json(array_map(PetResponse::data(...), $this->pets->list($account)));
    }

    #[Route('', name: 'api_pets_create', methods: ['POST'])]
    public function create(Request $request, #[CurrentUser] Account $account): JsonResponse
    {
        $input = JsonInput::read($request);

        return $this->idempotency->create(
            $account,
            $request->headers->get('Idempotency-Key'),
            $request->getPathInfo(),
            $input,
            function () use ($account, $input): JsonResponse {
                $pet = $this->pets->create($account, $input);

                return ApiResponse::resource(PetResponse::data($pet), $pet, 201);
            },
        );
    }

    #[Route('/{id}', name: 'api_pets_get', methods: ['GET'])]
    public function get(string $id, #[CurrentUser] Account $account): JsonResponse
    {
        $pet = $this->pets->get($account, $id);

        return ApiResponse::resource(PetResponse::data($pet), $pet);
    }

    #[Route('/{id}', name: 'api_pets_update', methods: ['PATCH'])]
    public function update(string $id, Request $request, #[CurrentUser] Account $account): JsonResponse
    {
        $pet = $this->pets->update($account, $id, JsonInput::read($request), $request->headers->get('If-Match'));

        return ApiResponse::resource(PetResponse::data($pet), $pet);
    }

    #[Route('/{id}', name: 'api_pets_delete', methods: ['DELETE'])]
    public function delete(string $id, Request $request, #[CurrentUser] Account $account): Response
    {
        $this->pets->delete($account, $id, $request->headers->get('If-Match'));

        return new Response(null, 204, ['Cache-Control' => 'no-store', 'Pragma' => 'no-cache']);
    }
}
