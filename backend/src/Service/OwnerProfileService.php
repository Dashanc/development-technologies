<?php

declare(strict_types=1);

namespace App\Service;

use App\Entity\Account;
use App\Entity\Owner;
use App\Exception\ApiError;
use App\Model\ResourceVersion;
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

    public function update(Account $account, array $input, ?string $ifMatch): Owner
    {
        try {
            return $this->entityManager->wrapInTransaction(function () use ($account, $input, $ifMatch): Owner {
                $owner = $this->owners->findForAccount($account, true) ?? throw new \LogicException('The account has no owner profile.');
                ResourceVersion::requireMatch($owner, $ifMatch);
                $data = $this->validator->profileUpdate($input, $owner);
                $account->setEmail($data['email']);
                $owner->update($data['name'], $data['phone']);

                return $owner;
            });
        } catch (UniqueConstraintViolationException) {
            throw new ApiError(409, 'EMAIL_ALREADY_USED', 'error.email_already_used', ['email' => 'validation.email_already_used']);
        }
    }
}
