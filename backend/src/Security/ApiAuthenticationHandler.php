<?php

declare(strict_types=1);

namespace App\Security;

use App\Http\ApiResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Security\Core\Exception\AccessDeniedException;
use Symfony\Component\Security\Core\Exception\AuthenticationException;
use Symfony\Component\Security\Http\Authentication\AuthenticationFailureHandlerInterface;
use Symfony\Component\Security\Http\Authorization\AccessDeniedHandlerInterface;
use Symfony\Component\Security\Http\EntryPoint\AuthenticationEntryPointInterface;

final class ApiAuthenticationHandler implements AuthenticationEntryPointInterface, AuthenticationFailureHandlerInterface, AccessDeniedHandlerInterface
{
    public function start(Request $request, ?AuthenticationException $authException = null): Response
    {
        return $this->unauthorized('AUTHENTICATION_REQUIRED', 'Sign in to access this resource.');
    }

    public function onAuthenticationFailure(Request $request, AuthenticationException $exception): Response
    {
        return $this->unauthorized('INVALID_SESSION', 'The session is invalid or has expired. Please sign in again.');
    }

    public function handle(Request $request, AccessDeniedException $accessDeniedException): ?Response
    {
        return ApiResponse::error(403, 'ACCESS_DENIED', 'You do not have access to this resource.');
    }

    private function unauthorized(string $code, string $message): Response
    {
        $response = ApiResponse::error(401, $code, $message);
        $response->headers->set('WWW-Authenticate', 'Bearer');

        return $response;
    }
}
