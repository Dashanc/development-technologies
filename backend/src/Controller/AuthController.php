<?php

declare(strict_types=1);

namespace App\Controller;

use App\Entity\AuthSession;
use App\Http\ApiResponse;
use App\Http\JsonInput;
use App\Security\AccessTokenHandler;
use App\Service\AuthService;
use App\Service\OwnerProfileService;
use App\Service\SessionService;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\HttpKernel\Attribute\AsController;
use Symfony\Component\Routing\Attribute\Route;

#[AsController]
#[Route('/api/v1/auth')]
final class AuthController
{
    public function __construct(
        private readonly AuthService $auth,
        private readonly SessionService $sessions,
        private readonly OwnerProfileService $profiles,
    ) {
    }

    #[Route('/register', name: 'api_auth_register', methods: ['POST'])]
    public function register(Request $request): JsonResponse
    {
        return ApiResponse::authentication($this->auth->register(JsonInput::read($request)), 201);
    }

    #[Route('/login', name: 'api_auth_login', methods: ['POST'])]
    public function login(Request $request): JsonResponse
    {
        return ApiResponse::authentication($this->auth->login(JsonInput::read($request)));
    }

    #[Route('/logout', name: 'api_auth_logout', methods: ['POST'])]
    public function logout(Request $request): Response
    {
        $this->sessions->revoke($this->currentSession($request));

        return new Response(null, 204, ['Cache-Control' => 'no-store', 'Pragma' => 'no-cache']);
    }

    #[Route('/session', name: 'api_auth_session', methods: ['GET'])]
    public function session(Request $request): JsonResponse
    {
        $session = $this->currentSession($request);

        return ApiResponse::json([
            'expiresAt' => $session->getExpiresAt()->format(DATE_ATOM),
            'owner' => ApiResponse::ownerData($this->profiles->get($session->getAccount())),
        ]);
    }

    private function currentSession(Request $request): AuthSession
    {
        $session = $request->attributes->get(AccessTokenHandler::SESSION_ATTRIBUTE);
        if (!$session instanceof AuthSession) {
            throw new \LogicException('The authenticated session is unavailable.');
        }

        return $session;
    }
}
