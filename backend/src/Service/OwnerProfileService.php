<?php

declare(strict_types=1);

namespace App\Service;

use App\Entity\Account;
use App\Entity\Owner;
use App\Exception\ApiError;
use App\Repository\OwnerRepository;
use Doctrine\DBAL\Exception\UniqueConstraintViolationException;
use Doctrine\ORM\EntityManagerInterface;

final class OwnerProfileService
{
    public function __construct(
        private readonly OwnerRepository $owners,
        private readonly OwnerInputValidator $validator,
        private readonly EntityManagerInterface $entityManager,
    ) {
    }

    public function get(Account $account): Owner
    {
        return $this->owners->findForAccount($account) ?? throw new \LogicException('The account has no owner profile.');
    }

    public function update(Account $account, array $input): Owner
    {
        $owner = $this->get($account);
        $data = $this->validator->profileUpdate($input, $owner);

        try {
            $this->entityManager->wrapInTransaction(function () use ($account, $owner, $data): void {
                $account->setEmail($data['email']);
                $owner->update($data['name'], $data['phone']);
            });
        } catch (UniqueConstraintViolationException) {
            throw new ApiError(409, 'EMAIL_ALREADY_USED', 'error.email_already_used', ['email' => 'validation.email_already_used']);
        }

        return $owner;
    }
}
