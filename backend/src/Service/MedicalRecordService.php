<?php

declare(strict_types=1);

namespace App\Service;

use App\Entity\Account;
use App\Entity\MedicalRecord;
use App\Exception\ApiError;
use App\Model\MedicalRecordType;
use App\Model\ResourceVersion;
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
        return $this->snapshot(function () use ($account, $petId, $query): array {
            $pet = $this->pets->get($account, $petId);

            return $this->records->history($pet, $this->validator->filter($query, true));
        });
    }

    public function health(Account $account, string $petId): array
    {
        return $this->snapshot(function () use ($account, $petId): array {
            $pet = $this->pets->get($account, $petId);
            $today = new \DateTimeImmutable('today', new \DateTimeZone('UTC'));

            return [
                'latestWeight' => $this->records->latestWeight($pet),
                'nextVaccination' => $this->records->nextVaccination($pet, $today),
                'nextAppointment' => $this->records->nextAppointment($pet, $today),
            ];
        });
    }

    public function get(Account $account, string $petId, MedicalRecordType $type, string $id, bool $forUpdate = false): MedicalRecord
    {
        $pet = $this->pets->get($account, $petId, $forUpdate);

        return $this->records->findForPet($pet, $type, $id, $forUpdate)
            ?? throw new ApiError(404, 'RECORD_NOT_FOUND', 'error.record_not_found');
    }

    public function create(Account $account, string $petId, MedicalRecordType $type, array $input): MedicalRecord
    {
        return $this->entityManager->wrapInTransaction(function () use ($account, $petId, $type, $input): MedicalRecord {
            $pet = $this->pets->get($account, $petId, true);
            $class = $type->entityClass();
            $record = new $class($pet, $this->validator->validate($type, $input));
            $this->entityManager->persist($record);

            return $record;
        });
    }

    public function update(Account $account, string $petId, MedicalRecordType $type, string $id, array $input, ?string $ifMatch): MedicalRecord
    {
        return $this->entityManager->wrapInTransaction(function () use ($account, $petId, $type, $id, $input, $ifMatch): MedicalRecord {
            $record = $this->get($account, $petId, $type, $id, true);
            ResourceVersion::requireMatch($record, $ifMatch);
            $record->update($this->validator->validate($type, $input, $record));

            return $record;
        });
    }

    public function delete(Account $account, string $petId, MedicalRecordType $type, string $id, ?string $ifMatch): void
    {
        $this->entityManager->wrapInTransaction(function () use ($account, $petId, $type, $id, $ifMatch): void {
            $record = $this->get($account, $petId, $type, $id, true);
            ResourceVersion::requireMatch($record, $ifMatch);
            $this->entityManager->remove($record);
        });
    }

    private function snapshot(callable $read): array
    {
        $connection = $this->entityManager->getConnection();

        return $connection->transactional(function () use ($connection, $read): array {
            $connection->executeStatement('SET TRANSACTION ISOLATION LEVEL REPEATABLE READ READ ONLY');

            return $read();
        });
    }
}
