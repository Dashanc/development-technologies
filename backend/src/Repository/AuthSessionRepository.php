<?php

declare(strict_types=1);

namespace App\Repository;

use App\Entity\AuthSession;
use Doctrine\Bundle\DoctrineBundle\Repository\ServiceEntityRepository;
use Doctrine\DBAL\Types\Types;
use Doctrine\Persistence\ManagerRegistry;

final class AuthSessionRepository extends ServiceEntityRepository
{
    public function __construct(ManagerRegistry $registry)
    {
        parent::__construct($registry, AuthSession::class);
    }

    public function findActive(#[\SensitiveParameter] string $token): ?AuthSession
    {
        if (preg_match('/^[a-f0-9]{64}$/D', $token) !== 1) {
            return null;
        }

        return $this->createQueryBuilder('session')
            ->addSelect('account')
            ->join('session.account', 'account')
            ->where('session.tokenHash = :hash')
            ->andWhere('session.expiresAt > :now')
            ->setParameter('hash', hash('sha256', $token))
            ->setParameter('now', new \DateTimeImmutable('now', new \DateTimeZone('UTC')), Types::DATETIMETZ_IMMUTABLE)
            ->getQuery()
            ->getOneOrNullResult();
    }
}
