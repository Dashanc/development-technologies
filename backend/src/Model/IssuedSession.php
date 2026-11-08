<?php

declare(strict_types=1);

namespace App\Model;

use App\Entity\AuthSession;

final readonly class IssuedSession
{
    public function __construct(public AuthSession $session, #[\SensitiveParameter] private string $token)
    {
    }

    public function getToken(): string
    {
        return $this->token;
    }

    public function __debugInfo(): array
    {
        return ['expiresAt' => $this->session->getExpiresAt()];
    }
}
