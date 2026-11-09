<?php

declare(strict_types=1);

namespace App\Repository;

use App\Entity\Owner;
use App\Entity\Pet;
use Doctrine\Bundle\DoctrineBundle\Repository\ServiceEntityRepository;
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

    public function findForOwner(Owner $owner, string $id): ?Pet
    {
        if (!Uuid::isValid($id)) {
            return null;
        }

        return $this->findOneBy(['owner' => $owner, 'id' => Uuid::fromString($id)]);
    }
}
