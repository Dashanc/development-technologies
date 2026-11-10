<?php

declare(strict_types=1);

namespace App\Http;

use App\Entity\MedicalRecord;
use App\Entity\VaccinationRecord;
use App\Entity\VetVisitRecord;
use App\Entity\WeightRecord;
use App\Model\MedicalRecordType;

final class MedicalRecordResponse
{
    public static function data(MedicalRecord $record): array
    {
        $data = [
            'id' => $record->getId()->toRfc4122(),
            'petId' => $record->getPet()->getId()->toRfc4122(),
            'type' => MedicalRecordType::forRecord($record)->value,
            'date' => $record->getDate()->format('Y-m-d'),
            'note' => $record->getNote(),
        ];
        if ($record instanceof WeightRecord) {
            $data['weightKg'] = (float) $record->getWeightKg();
        } elseif ($record instanceof VaccinationRecord) {
            $data += [
                'vaccine' => $record->getVaccine(),
                'nextVaccinationDate' => $record->getNextVaccinationDate()?->format('Y-m-d'),
                'clinic' => $record->getClinic(),
                'veterinarian' => $record->getVeterinarian(),
            ];
        } elseif ($record instanceof VetVisitRecord) {
            $data += [
                'reason' => $record->getReason(),
                'clinic' => $record->getClinic(),
                'veterinarian' => $record->getVeterinarian(),
                'diagnosis' => $record->getDiagnosis(),
                'recommendations' => $record->getRecommendations(),
                'appointmentDate' => $record->getAppointmentDate()?->format('Y-m-d'),
            ];
        }

        return $data;
    }
}
