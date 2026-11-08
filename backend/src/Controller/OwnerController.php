<?php

declare(strict_types=1);

namespace App\Controller;

use App\Entity\Account;
use App\Http\ApiResponse;
use App\Http\JsonInput;
use App\Service\OwnerProfileService;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpKernel\Attribute\AsController;
use Symfony\Component\Routing\Attribute\Route;
use Symfony\Component\Security\Http\Attribute\CurrentUser;

#[AsController]
#[Route('/api/v1/owner')]
final class OwnerController
{
    public function __construct(private readonly OwnerProfileService $profiles)
    {
    }

    #[Route('', name: 'api_owner_get', methods: ['GET'])]
    public function get(#[CurrentUser] Account $account): JsonResponse
    {
        return ApiResponse::json(ApiResponse::ownerData($this->profiles->get($account)));
    }

    #[Route('', name: 'api_owner_update', methods: ['PATCH'])]
    public function update(Request $request, #[CurrentUser] Account $account): JsonResponse
    {
        return ApiResponse::json(ApiResponse::ownerData($this->profiles->update($account, JsonInput::read($request))));
    }
}
