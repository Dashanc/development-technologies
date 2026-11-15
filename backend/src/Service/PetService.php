<?php

declare(strict_types=1);

namespace App\Service;

use App\Entity\Account;
use App\Entity\Pet;
use App\Exception\ApiError;
use App\Model\ResourceVersion;
use App\Repository\PetRepository;
use Doctrine\ORM\EntityManagerInterface;

final class PetService
{
    public function __construct(
        private readonly PetRepository $pets,
        private readonly OwnerProfileService $profiles,
        private readonly PetInputValidator $validator,
        private readonly EntityManagerInterface $entityManager,
    ) {
    }

    public function list(Account $account): array
    {
        return $this->pets->listForOwner($this->profiles->get($account));
    }

    public function get(Account $account, string $id, bool $forUpdate = false): Pet
    {
        return $this->pets->findForOwner($this->profiles->get($account), $id, $forUpdate)
            ?? throw new ApiError(404, 'PET_NOT_FOUND', 'error.pet_not_found');
    }

    public function create(Account $account, array $input): Pet
    {
        $pet = new Pet($this->profiles->get($account), $this->validator->validate($input));
        $this->entityManager->wrapInTransaction(function () use ($pet): void {
            $this->entityManager->persist($pet);
        });

        return $pet;
    }

    public function update(Account $account, string $id, array $input, ?string $ifMatch): Pet
    {
        return $this->entityManager->wrapInTransaction(function () use ($account, $id, $input, $ifMatch): Pet {
            $pet = $this->get($account, $id, true);
            ResourceVersion::requireMatch($pet, $ifMatch);
            $pet->update($this->validator->validate($input, $pet));

            return $pet;
        });
    }

    public function delete(Account $account, string $id, ?string $ifMatch): void
    {
        $this->entityManager->wrapInTransaction(function () use ($account, $id, $ifMatch): void {
            $pet = $this->get($account, $id, true);
            ResourceVersion::requireMatch($pet, $ifMatch);
            $this->entityManager->remove($pet);
        });
    }
}
