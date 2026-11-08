<?php

declare(strict_types=1);

namespace App\Service;

use App\Entity\Account;
use App\Entity\Owner;
use App\Exception\ApiError;
use App\Model\AuthenticationResult;
use App\Repository\AccountRepository;
use Doctrine\DBAL\Exception\UniqueConstraintViolationException;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Component\PasswordHasher\Hasher\UserPasswordHasherInterface;

final class AuthService
{
    public function __construct(
        private readonly AccountRepository $accounts,
        private readonly OwnerInputValidator $validator,
        private readonly UserPasswordHasherInterface $passwordHasher,
        private readonly OwnerProfileService $profiles,
        private readonly SessionService $sessions,
        private readonly EntityManagerInterface $entityManager,
    ) {
    }

    public function register(#[\SensitiveParameter] array $input): AuthenticationResult
    {
        $data = $this->validator->registration($input);
        $account = new Account($data['email']);
        $account->setPasswordHash($this->passwordHasher->hashPassword($account, $data['password']));
        $owner = new Owner($account, $data['name'], $data['phone']);

        try {
            return $this->entityManager->wrapInTransaction(function () use ($account, $owner): AuthenticationResult {
                $this->entityManager->persist($account);
                $this->entityManager->persist($owner);

                return new AuthenticationResult($owner, $this->sessions->issue($account));
            });
        } catch (UniqueConstraintViolationException) {
            throw new ApiError(409, 'EMAIL_ALREADY_USED', 'This email address is already in use.', ['email' => 'Choose another email address.']);
        }
    }

    public function login(#[\SensitiveParameter] array $input): AuthenticationResult
    {
        $data = $this->validator->login($input);
        $account = $this->accounts->findByEmail($data['email']);
        if ($account === null) {
            $this->passwordHasher->hashPassword(new Account($data['email']), $data['password']);
            throw new ApiError(401, 'INVALID_CREDENTIALS', 'The email address or password is incorrect.');
        }
        if (!$this->passwordHasher->isPasswordValid($account, $data['password'])) {
            throw new ApiError(401, 'INVALID_CREDENTIALS', 'The email address or password is incorrect.');
        }

        $owner = $this->profiles->get($account);

        return $this->entityManager->wrapInTransaction(function () use ($account, $owner, $data): AuthenticationResult {
            if ($this->passwordHasher->needsRehash($account)) {
                $account->setPasswordHash($this->passwordHasher->hashPassword($account, $data['password']));
            }

            return new AuthenticationResult($owner, $this->sessions->issue($account));
        });
    }
}
