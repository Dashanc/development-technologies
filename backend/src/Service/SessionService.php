<?php

declare(strict_types=1);

namespace App\Service;

use App\Entity\Account;
use App\Entity\AuthSession;
use App\Model\IssuedSession;
use Doctrine\ORM\EntityManagerInterface;

final class SessionService
{
    public function __construct(private readonly EntityManagerInterface $entityManager)
    {
    }

    public function issue(Account $account): IssuedSession
    {
        $token = bin2hex(random_bytes(32));
        $now = new \DateTimeImmutable('now', new \DateTimeZone('UTC'));
        $session = new AuthSession(hash('sha256', $token), $account, $now, $now->add(new \DateInterval('P30D')));
        $this->entityManager->persist($session);

        return new IssuedSession($session, $token);
    }

    public function revoke(AuthSession $session): void
    {
        $this->entityManager->remove($session);
        $this->entityManager->flush();
    }
}
