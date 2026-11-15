<?php

declare(strict_types=1);

namespace App\Repository;

use App\Entity\MedicalRecord;
use App\Entity\Pet;
use App\Entity\VaccinationRecord;
use App\Entity\VetVisitRecord;
use App\Entity\WeightRecord;
use App\Model\MedicalRecordFilter;
use App\Model\MedicalRecordType;
use Doctrine\DBAL\LockMode;
use Doctrine\DBAL\Types\Types;
use Doctrine\ORM\EntityManagerInterface;
use Doctrine\ORM\Query;
use Doctrine\ORM\QueryBuilder;
use Symfony\Component\Uid\Uuid;

final class MedicalRecordRepository
{
    public function __construct(private readonly EntityManagerInterface $entityManager)
    {
    }

    public function findForPet(Pet $pet, MedicalRecordType $type, string $id, bool $forUpdate = false): ?MedicalRecord
    {
        if (!Uuid::isValid($id)) {
            return null;
        }

        $query = $this->query($pet, $type->entityClass())->andWhere('r.id = :id')
            ->setParameter('id', Uuid::fromString($id), 'uuid')->getQuery();
        if ($forUpdate) {
            $query->setHint(Query::HINT_REFRESH, true)->setLockMode(LockMode::PESSIMISTIC_WRITE);
        }

        return $query->getOneOrNullResult();
    }

    public function listForPet(Pet $pet, MedicalRecordType $type, MedicalRecordFilter $filter): array
    {
        $query = $this->query($pet, $type->entityClass())->orderBy('r.date', 'DESC')->addOrderBy('r.id', 'DESC');
        if ($filter->from !== null) {
            $query->andWhere('r.date >= :from')->setParameter('from', $filter->from, Types::DATE_IMMUTABLE);
        }
        if ($filter->to !== null) {
            $query->andWhere('r.date <= :to')->setParameter('to', $filter->to, Types::DATE_IMMUTABLE);
        }

        return $query->getQuery()->getResult();
    }

    public function history(Pet $pet, MedicalRecordFilter $filter): array
    {
        $records = [];
        foreach ($filter->type !== null ? [$filter->type] : MedicalRecordType::cases() as $type) {
            array_push($records, ...$this->listForPet($pet, $type, $filter));
        }
        usort($records, static fn (MedicalRecord $left, MedicalRecord $right): int =>
            ($right->getDate() <=> $left->getDate()) ?: strcmp($right->getId()->toRfc4122(), $left->getId()->toRfc4122()));

        return $records;
    }

    public function latestWeight(Pet $pet): ?WeightRecord
    {
        return $this->query($pet, WeightRecord::class)->orderBy('r.date', 'DESC')->addOrderBy('r.id', 'DESC')
            ->setMaxResults(1)->getQuery()->getOneOrNullResult();
    }

    public function nextVaccination(Pet $pet, \DateTimeImmutable $today): ?VaccinationRecord
    {
        return $this->query($pet, VaccinationRecord::class)->andWhere('r.nextVaccinationDate >= :today')
            ->setParameter('today', $today, Types::DATE_IMMUTABLE)->orderBy('r.nextVaccinationDate', 'ASC')->addOrderBy('r.id', 'ASC')
            ->setMaxResults(1)->getQuery()->getOneOrNullResult();
    }

    public function nextAppointment(Pet $pet, \DateTimeImmutable $today): ?VetVisitRecord
    {
        return $this->query($pet, VetVisitRecord::class)->andWhere('r.appointmentDate >= :today')
            ->setParameter('today', $today, Types::DATE_IMMUTABLE)->orderBy('r.appointmentDate', 'ASC')->addOrderBy('r.id', 'ASC')
            ->setMaxResults(1)->getQuery()->getOneOrNullResult();
    }

    private function query(Pet $pet, string $entityClass): QueryBuilder
    {
        return $this->entityManager->createQueryBuilder()->select('r')->from($entityClass, 'r')
            ->where('r.pet = :pet')->setParameter('pet', $pet);
    }
}
