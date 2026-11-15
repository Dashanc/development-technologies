<?php

declare(strict_types=1);

namespace App\Repository;

use App\Entity\Account;
use App\Entity\Owner;
use Doctrine\Bundle\DoctrineBundle\Repository\ServiceEntityRepository;
use Doctrine\DBAL\LockMode;
use Doctrine\ORM\Query;
use Doctrine\Persistence\ManagerRegistry;

final class OwnerRepository extends ServiceEntityRepository
{
    public function __construct(ManagerRegistry $registry)
    {
        parent::__construct($registry, Owner::class);
    }

    public function findForAccount(Account $account, bool $forUpdate = false): ?Owner
    {
        $query = $this->createQueryBuilder('owner')->addSelect('account')->join('owner.account', 'account')
            ->where('owner.account = :account')->setParameter('account', $account)->getQuery();
        if ($forUpdate) {
            $query->setHint(Query::HINT_REFRESH, true)->setLockMode(LockMode::PESSIMISTIC_WRITE);
        }

        return $query->getOneOrNullResult();
    }
}
