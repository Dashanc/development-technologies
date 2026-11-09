<?php

declare(strict_types=1);

namespace App\Service;

use App\Entity\Pet;
use App\Exception\ApiError;

final class PetInputValidator
{
    public function validate(array $input, ?Pet $pet = null): array
    {
        $errors = [];
        $this->unknownFields($input, ['name', 'species', 'breed', 'birthDate', 'sex', 'extra'], '', $errors);
        if ($pet !== null && $input === []) {
            $errors['_request'] = 'validation.pet_field_required';
        }
        $data = $pet === null ? [
            'name' => '', 'species' => '', 'breed' => null, 'birthDate' => null, 'sex' => '',
            'extra' => ['coat' => null, 'microchip' => null, 'passportNumber' => null, 'neutered' => false, 'allergies' => null],
        ] : [
            'name' => $pet->getName(), 'species' => $pet->getSpecies(), 'breed' => $pet->getBreed(),
            'birthDate' => $pet->getBirthDate()?->format('Y-m-d'), 'sex' => $pet->getSex(), 'extra' => $pet->getExtra(),
        ];
        foreach (['name', 'species', 'breed', 'birthDate', 'sex'] as $field) {
            if (array_key_exists($field, $input)) {
                $data[$field] = $input[$field];
            }
        }
        $data['name'] = $this->text($data['name'], 'name', false, $errors);
        if ($data['name'] === '') {
            $errors['name'] ??= 'validation.pet_name_required';
        }
        $data['breed'] = $this->text($data['breed'], 'breed', true, $errors);
        foreach (['species' => ['CAT', 'DOG', 'OTHER'], 'sex' => ['FEMALE', 'MALE', 'UNKNOWN']] as $field => $values) {
            if (!is_string($data[$field]) || !in_array($data[$field], $values, true)) {
                $errors[$field] = 'validation.'.$field.'_invalid';
            }
        }
        $birthDate = $data['birthDate'];
        $data['birthDate'] = null;
        if (is_string($birthDate) && preg_match('/^[0-9]{4}-[0-9]{2}-[0-9]{2}$/D', $birthDate) === 1) {
            $date = \DateTimeImmutable::createFromFormat('!Y-m-d', $birthDate, new \DateTimeZone('UTC'));
            if ($date !== false && $date->format('Y-m-d') === $birthDate && $date->format('Y') !== '0000') {
                $data['birthDate'] = $date;
            }
        }
        if ($data['birthDate'] === null) {
            $errors['birthDate'] = 'validation.birth_date_invalid';
        } elseif ($data['birthDate'] > new \DateTimeImmutable('today', new \DateTimeZone('UTC'))) {
            $errors['birthDate'] = 'validation.birth_date_future';
        }
        if (array_key_exists('extra', $input)) {
            if (!$input['extra'] instanceof \stdClass) {
                $errors['extra'] = 'validation.object_required';
            } else {
                $extra = get_object_vars($input['extra']);
                $this->unknownFields($extra, array_keys($data['extra']), 'extra.', $errors);
                $data['extra'] = array_replace($data['extra'], array_intersect_key($extra, $data['extra']));
            }
        }
        foreach (['coat', 'microchip', 'passportNumber', 'allergies'] as $field) {
            $data['extra'][$field] = $this->text($data['extra'][$field], 'extra.'.$field, true, $errors);
        }
        if ($data['extra']['microchip'] !== null && preg_match('/^[0-9]{15}$/D', $data['extra']['microchip']) !== 1) {
            $errors['extra.microchip'] = 'validation.microchip_invalid';
        }
        if (!is_bool($data['extra']['neutered'])) {
            $errors['extra.neutered'] = 'validation.boolean_required';
        }
        if ($errors !== []) {
            throw new ApiError(422, 'VALIDATION_FAILED', 'error.validation_failed', $errors);
        }

        return $data;
    }

    private function text(mixed $value, string $field, bool $nullable, array &$errors): ?string
    {
        if ($nullable && $value === null) {
            return null;
        }
        if (!is_string($value)) {
            $errors[$field] = 'validation.string_required';

            return $nullable ? null : '';
        }
        $value = preg_replace('/^[\s\p{Z}]+|[\s\p{Z}]+$/u', '', $value) ?? '';
        if (str_contains($value, "\0")) {
            $errors[$field] = 'validation.invalid_characters';
        }

        return $nullable && $value === '' ? null : $value;
    }

    private function unknownFields(array $input, array $allowed, string $prefix, array &$errors): void
    {
        foreach (array_diff(array_keys($input), $allowed) as $field) {
            $errors[$prefix.$field] = 'validation.field_not_supported';
        }
    }
}
