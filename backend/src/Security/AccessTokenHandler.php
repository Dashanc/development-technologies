<?php

declare(strict_types=1);

namespace App\Security;

use App\Repository\AuthSessionRepository;
use Symfony\Component\HttpFoundation\RequestStack;
use Symfony\Component\Security\Core\Exception\BadCredentialsException;
use Symfony\Component\Security\Http\AccessToken\AccessTokenHandlerInterface;
use Symfony\Component\Security\Http\Authenticator\Passport\Badge\UserBadge;

final class AccessTokenHandler implements AccessTokenHandlerInterface
{
    public const SESSION_ATTRIBUTE = '_authenticated_session';

    public function __construct(private readonly AuthSessionRepository $sessions, private readonly RequestStack $requests)
    {
    }

    public function getUserBadgeFrom(#[\SensitiveParameter] string $accessToken): UserBadge
    {
        $session = $this->sessions->findActive($accessToken);
        if ($session === null) {
            throw new BadCredentialsException('Invalid session.');
        }

        $this->requests->getCurrentRequest()?->attributes->set(self::SESSION_ATTRIBUTE, $session);

        return new UserBadge($session->getAccount()->getUserIdentifier(), fn () => $session->getAccount());
    }
}
