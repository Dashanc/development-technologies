<?php

declare(strict_types=1);

namespace App\Service;

use App\Entity\Account;
use App\Entity\MedicalRecord;
use App\Exception\ApiError;
use App\Model\MedicalRecordType;
use App\Repository\MedicalRecordRepository;
use Doctrine\ORM\EntityManagerInterface;

final class MedicalRecordService
{
    public function __construct(
        private readonly PetService $pets,
        private readonly MedicalRecordRepository $records,
        private readonly MedicalRecordInputValidator $validator,
        private readonly EntityManagerInterface $entityManager,
    ) {
    }

    public function list(Account $account, string $petId, MedicalRecordType $type, array $query): array
    {
        $pet = $this->pets->get($account, $petId);

        return $this->records->listForPet($pet, $type, $this->validator->filter($query, false));
    }

    public function history(Account $account, string $petId, array $query): array
    {
        $pet = $this->pets->get($account, $petId);

        return $this->records->history($pet, $this->validator->filter($query, true));
    }

    public function health(Account $account, string $petId): array
    {
        $pet = $this->pets->get($account, $petId);
        $today = new \DateTimeImmutable('today', new \DateTimeZone('UTC'));

        return [
            'latestWeight' => $this->records->latestWeight($pet),
            'nextVaccination' => $this->records->nextVaccination($pet, $today),
            'nextAppointment' => $this->records->nextAppointment($pet, $today),
        ];
    }

    public function get(Account $account, string $petId, MedicalRecordType $type, string $id): MedicalRecord
    {
        $pet = $this->pets->get($account, $petId);

        return $this->records->findForPet($pet, $type, $id)
            ?? throw new ApiError(404, 'RECORD_NOT_FOUND', 'error.record_not_found');
    }

    public function create(Account $account, string $petId, MedicalRecordType $type, array $input): MedicalRecord
    {
        $pet = $this->pets->get($account, $petId);
        $class = $type->entityClass();
        $record = new $class($pet, $this->validator->validate($type, $input));
        $this->entityManager->wrapInTransaction(function () use ($record): void {
            $this->entityManager->persist($record);
        });

        return $record;
    }

    public function update(Account $account, string $petId, MedicalRecordType $type, string $id, array $input): MedicalRecord
    {
        $record = $this->get($account, $petId, $type, $id);
        $data = $this->validator->validate($type, $input, $record);
        $this->entityManager->wrapInTransaction(function () use ($record, $data): void {
            $record->update($data);
        });

        return $record;
    }

    public function delete(Account $account, string $petId, MedicalRecordType $type, string $id): void
    {
        $record = $this->get($account, $petId, $type, $id);
        $this->entityManager->wrapInTransaction(function () use ($record): void {
            $this->entityManager->remove($record);
        });
    }
}
