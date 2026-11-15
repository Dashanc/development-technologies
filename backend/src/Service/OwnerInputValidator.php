<?php

declare(strict_types=1);

namespace App\Service;

use App\Entity\Owner;
use App\Exception\ApiError;
use Symfony\Component\Translation\TranslatableMessage;
use Symfony\Component\Validator\Constraints as Assert;
use Symfony\Component\Validator\Validator\ValidatorInterface;

final class OwnerInputValidator
{
    public function __construct(private readonly ValidatorInterface $validator)
    {
    }

    public function registration(#[\SensitiveParameter] array $input): array
    {
        $errors = $this->unknownFields($input, ['name', 'email', 'phone', 'password']);
        $data = $this->profileFields($input, null, $errors);
        $data['password'] = $this->password($input['password'] ?? null, 8, $errors);
        $this->check($errors);

        return $data;
    }

    public function login(#[\SensitiveParameter] array $input): array
    {
        $errors = $this->unknownFields($input, ['email', 'password']);
        $email = $this->email($input['email'] ?? null, $errors);
        $password = $this->password($input['password'] ?? null, 1, $errors);
        $this->check($errors);

        return ['email' => $email, 'password' => $password];
    }

    public function profileUpdate(array $input, Owner $owner): array
    {
        $errors = $this->unknownFields($input, ['name', 'email', 'phone']);
        if ($input === []) {
            $errors['_request'] = 'validation.profile_field_required';
        }
        $data = $this->profileFields($input, $owner, $errors);
        $this->check($errors);

        return $data;
    }

    private function profileFields(#[\SensitiveParameter] array $input, ?Owner $owner, array &$errors): array
    {
        $name = $this->text(array_key_exists('name', $input) ? $input['name'] : ($owner?->getName() ?? ''), 'name', $errors);
        $this->validate($name, 'name', [new Assert\NotBlank(message: 'validation.name_required')], $errors);
        $email = $this->email(array_key_exists('email', $input) ? $input['email'] : ($owner?->getAccount()->getEmail() ?? ''), $errors);
        $phone = $this->phone(array_key_exists('phone', $input) ? $input['phone'] : $owner?->getPhone(), $errors);

        return ['name' => $name, 'email' => $email, 'phone' => $phone];
    }

    private function email(mixed $value, array &$errors): string
    {
        $email = strtolower($this->text($value, 'email', $errors));
        $this->validate($email, 'email', [
            new Assert\NotBlank(message: 'validation.email_required'),
            new Assert\Length(max: 254, maxMessage: 'validation.too_long', charsetMessage: 'validation.invalid_characters'),
            new Assert\Email(message: 'validation.email_invalid'),
        ], $errors);

        return $email;
    }

    private function text(mixed $value, string $field, array &$errors): string
    {
        if (!is_string($value)) {
            $errors[$field] = 'validation.string_required';

            return '';
        }

        $value = preg_replace('/^[\s\p{Z}]+|[\s\p{Z}]+$/u', '', $value) ?? '';
        if (str_contains($value, "\0")) {
            $errors[$field] = 'validation.invalid_characters';
        }

        return $value;
    }

    private function password(#[\SensitiveParameter] mixed $value, int $minimum, array &$errors): string
    {
        if (!is_string($value)) {
            $errors['password'] = 'validation.password_required';

            return '';
        }

        if (str_contains($value, "\0")) {
            $errors['password'] = 'validation.invalid_characters';
        }

        $this->validate($value, 'password', [new Assert\Length(
            min: $minimum,
            max: 128,
            minMessage: 'validation.too_short',
            maxMessage: 'validation.too_long',
            charsetMessage: 'validation.invalid_characters',
        )], $errors);

        return $value;
    }

    private function phone(mixed $value, array &$errors): ?string
    {
        if ($value === null || (is_string($value) && in_array(trim($value), ['', '+7', '+7 ('], true))) {
            return null;
        }

        if (is_string($value) && preg_match('/^[0-9+() -]+$/D', $value) === 1) {
            $digits = preg_replace('/[^0-9]/', '', $value);
            $validPlus = substr_count($value, '+') <= 1 && (!str_contains($value, '+') || str_starts_with(trim($value), '+7'));
            if ($validPlus && strlen($digits) === 11 && in_array($digits[0], ['7', '8'], true)) {
                return '+7'.substr($digits, 1);
            }
            if ($validPlus && strlen($digits) === 10 && !str_contains($value, '+')) {
                return '+7'.$digits;
            }
        }

        $errors['phone'] = 'validation.phone_invalid';

        return null;
    }

    private function unknownFields(#[\SensitiveParameter] array $input, array $allowed): array
    {
        $errors = [];
        foreach (array_diff(array_keys($input), $allowed) as $field) {
            $errors[$field] = 'validation.field_not_supported';
        }

        return $errors;
    }

    private function validate(#[\SensitiveParameter] string $value, string $field, array $constraints, array &$errors): void
    {
        foreach ($this->validator->validate($value, $constraints) as $violation) {
            $errors[$field] ??= new TranslatableMessage(
                $violation->getMessageTemplate(),
                array_intersect_key($violation->getParameters(), ['{{ limit }}' => true]),
                'api',
            );
        }
    }

    private function check(array $errors): void
    {
        if ($errors !== []) {
            throw new ApiError(422, 'VALIDATION_FAILED', 'error.validation_failed', $errors);
        }
    }
}
