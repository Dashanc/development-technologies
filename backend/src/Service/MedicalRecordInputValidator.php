<?php

declare(strict_types=1);

namespace App\Service;

use App\Entity\MedicalRecord;
use App\Entity\VaccinationRecord;
use App\Entity\VetVisitRecord;
use App\Entity\WeightRecord;
use App\Exception\ApiError;
use App\Model\MedicalRecordFilter;
use App\Model\MedicalRecordType;

final class MedicalRecordInputValidator
{
    public function validate(MedicalRecordType $type, array $input, ?MedicalRecord $record = null): array
    {
        $errors = [];
        $data = ['date' => $record?->getDate()->format('Y-m-d'), 'note' => $record?->getNote()];
        $data += match ($type) {
            MedicalRecordType::WEIGHT => ['weightKg' => $record instanceof WeightRecord ? (float) $record->getWeightKg() : null],
            MedicalRecordType::VACCINATION => [
                'vaccine' => $record instanceof VaccinationRecord ? $record->getVaccine() : '',
                'nextVaccinationDate' => $record instanceof VaccinationRecord ? $record->getNextVaccinationDate()?->format('Y-m-d') : null,
                'clinic' => $record instanceof VaccinationRecord ? $record->getClinic() : null,
                'veterinarian' => $record instanceof VaccinationRecord ? $record->getVeterinarian() : null,
            ],
            MedicalRecordType::VET_VISIT => [
                'reason' => $record instanceof VetVisitRecord ? $record->getReason() : '',
                'clinic' => $record instanceof VetVisitRecord ? $record->getClinic() : null,
                'veterinarian' => $record instanceof VetVisitRecord ? $record->getVeterinarian() : null,
                'diagnosis' => $record instanceof VetVisitRecord ? $record->getDiagnosis() : null,
                'recommendations' => $record instanceof VetVisitRecord ? $record->getRecommendations() : null,
                'appointmentDate' => $record instanceof VetVisitRecord ? $record->getAppointmentDate()?->format('Y-m-d') : null,
            ],
        };
        $this->unknownFields($input, array_keys($data), $errors);
        if ($record !== null && $input === []) {
            $errors['_request'] = 'validation.record_field_required';
        }
        $data = array_replace($data, array_intersect_key($input, $data));
        $data['date'] = $this->date($data['date'], 'date', false, $errors);
        if ($type !== MedicalRecordType::VET_VISIT && $data['date'] > $this->today()) {
            $errors['date'] = 'validation.record_date_future';
        }
        foreach (['note', 'clinic', 'veterinarian', 'diagnosis', 'recommendations'] as $field) {
            if (array_key_exists($field, $data)) {
                $data[$field] = $this->text($data[$field], $field, true, $errors);
            }
        }
        if ($type === MedicalRecordType::WEIGHT) {
            $weight = $data['weightKg'];
            if ((!is_int($weight) && !is_float($weight)) || !is_finite((float) $weight)
                || $weight < 0.001 || $weight > 99999.999 || abs($weight * 1000 - round($weight * 1000)) > 0.0000001) {
                $errors['weightKg'] = 'validation.weight_invalid';
            } else {
                $data['weightKg'] = number_format((float) $weight, 3, '.', '');
            }
        } elseif ($type === MedicalRecordType::VACCINATION) {
            $data['vaccine'] = $this->text($data['vaccine'], 'vaccine', false, $errors);
            $data['nextVaccinationDate'] = $this->date($data['nextVaccinationDate'], 'nextVaccinationDate', true, $errors);
            if ($data['date'] !== null && $data['nextVaccinationDate'] !== null && $data['nextVaccinationDate'] <= $data['date']) {
                $errors['nextVaccinationDate'] = 'validation.next_vaccination_after';
            }
        } else {
            $data['reason'] = $this->text($data['reason'], 'reason', false, $errors);
            $data['appointmentDate'] = $this->date($data['appointmentDate'], 'appointmentDate', true, $errors);
            if ($data['date'] !== null && $data['appointmentDate'] !== null && $data['appointmentDate'] < $data['date']) {
                $errors['appointmentDate'] = 'validation.appointment_not_before';
            }
        }
        $this->check($errors);

        return $data;
    }

    public function filter(array $query, bool $allowType): MedicalRecordFilter
    {
        $errors = [];
        $this->unknownFields($query, $allowType ? ['type', 'from', 'to'] : ['from', 'to'], $errors);
        $type = null;
        if ($allowType && array_key_exists('type', $query)) {
            $type = is_string($query['type']) ? MedicalRecordType::tryFrom($query['type']) : null;
            if ($type === null) {
                $errors['type'] = 'validation.record_type_invalid';
            }
        }
        $from = array_key_exists('from', $query) ? $this->date($query['from'], 'from', false, $errors) : null;
        $to = array_key_exists('to', $query) ? $this->date($query['to'], 'to', false, $errors) : null;
        if ($from !== null && $to !== null && $from > $to) {
            $errors['to'] = 'validation.date_range_invalid';
        }
        $this->check($errors);

        return new MedicalRecordFilter($type, $from, $to);
    }

    private function date(mixed $value, string $field, bool $nullable, array &$errors): ?\DateTimeImmutable
    {
        if ($value === null && $nullable) {
            return null;
        }
        if (is_string($value) && preg_match('/^[0-9]{4}-[0-9]{2}-[0-9]{2}$/D', $value) === 1) {
            $date = \DateTimeImmutable::createFromFormat('!Y-m-d', $value, new \DateTimeZone('UTC'));
            if ($date !== false && $date->format('Y-m-d') === $value && $date->format('Y') !== '0000') {
                return $date;
            }
        }
        $errors[$field] = 'validation.date_invalid';

        return null;
    }

    private function text(mixed $value, string $field, bool $nullable, array &$errors): ?string
    {
        if ($value === null && $nullable) {
            return null;
        }
        if (!is_string($value)) {
            $errors[$field] = 'validation.string_required';

            return $nullable ? null : '';
        }
        $value = preg_replace('/^[\s\p{Z}]+|[\s\p{Z}]+$/u', '', $value) ?? '';
        if (str_contains($value, "\0")) {
            $errors[$field] = 'validation.invalid_characters';
        } elseif (!$nullable && $value === '') {
            $errors[$field] = 'validation.record_text_required';
        }

        return $nullable && $value === '' ? null : $value;
    }

    private function unknownFields(array $input, array $allowed, array &$errors): void
    {
        foreach (array_diff(array_keys($input), $allowed) as $field) {
            $errors[$field] = 'validation.field_not_supported';
        }
    }

    private function today(): \DateTimeImmutable
    {
        return new \DateTimeImmutable('today', new \DateTimeZone('UTC'));
    }

    private function check(array $errors): void
    {
        if ($errors !== []) {
            throw new ApiError(422, 'VALIDATION_FAILED', 'error.validation_failed', $errors);
        }
    }
}
