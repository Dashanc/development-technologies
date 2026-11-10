<?php

declare(strict_types=1);

namespace App\Model;

use App\Entity\MedicalRecord;
use App\Entity\VaccinationRecord;
use App\Entity\VetVisitRecord;
use App\Entity\WeightRecord;

enum MedicalRecordType: string
{
    case WEIGHT = 'WEIGHT';
    case VACCINATION = 'VACCINATION';
    case VET_VISIT = 'VET_VISIT';

    public function entityClass(): string
    {
        return match ($this) {
            self::WEIGHT => WeightRecord::class,
            self::VACCINATION => VaccinationRecord::class,
            self::VET_VISIT => VetVisitRecord::class,
        };
    }

    public static function fromCollection(string $collection): self
    {
        return match ($collection) {
            'weight-records' => self::WEIGHT,
            'vaccination-records' => self::VACCINATION,
            'vet-visits' => self::VET_VISIT,
        };
    }

    public static function forRecord(MedicalRecord $record): self
    {
        return match (true) {
            $record instanceof WeightRecord => self::WEIGHT,
            $record instanceof VaccinationRecord => self::VACCINATION,
            $record instanceof VetVisitRecord => self::VET_VISIT,
        };
    }
}
