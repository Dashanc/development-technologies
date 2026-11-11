<?php

declare(strict_types=1);

namespace App\Repository;

use App\Entity\Owner;
use App\Entity\Pet;
use Doctrine\Bundle\DoctrineBundle\Repository\ServiceEntityRepository;
use Doctrine\DBAL\LockMode;
use Doctrine\ORM\Query;
use Doctrine\Persistence\ManagerRegistry;
use Symfony\Component\Uid\Uuid;

final class PetRepository extends ServiceEntityRepository
{
    public function __construct(ManagerRegistry $registry)
    {
        parent::__construct($registry, Pet::class);
    }

    public function listForOwner(Owner $owner): array
    {
        return $this->findBy(['owner' => $owner], ['id' => 'ASC']);
    }

    public function findForOwner(Owner $owner, string $id, bool $forUpdate = false): ?Pet
    {
        if (!Uuid::isValid($id)) {
            return null;
        }

        $query = $this->createQueryBuilder('pet')->where('pet.owner = :owner')->andWhere('pet.id = :id')
            ->setParameter('owner', $owner)->setParameter('id', Uuid::fromString($id), 'uuid')->getQuery();
        if ($forUpdate) {
            $query->setHint(Query::HINT_REFRESH, true)->setLockMode(LockMode::PESSIMISTIC_WRITE);
        }

        return $query->getOneOrNullResult();
    }
}
