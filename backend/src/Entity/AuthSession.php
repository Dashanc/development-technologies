<?php

declare(strict_types=1);

namespace App\Entity;

use Doctrine\DBAL\Types\Types;
use Doctrine\ORM\Mapping as ORM;

#[ORM\Entity]
#[ORM\Table(name: 'auth_sessions')]
#[ORM\Index(name: 'idx_auth_sessions_account', columns: ['account_id'])]
#[ORM\Index(name: 'idx_auth_sessions_expiry', columns: ['expires_at'])]
class AuthSession
{
    #[ORM\Id]
    #[ORM\Column(name: 'token_hash', length: 64, options: ['fixed' => true])]
    private string $tokenHash;

    #[ORM\ManyToOne(targetEntity: Account::class)]
    #[ORM\JoinColumn(name: 'account_id', nullable: false, onDelete: 'CASCADE')]
    private Account $account;

    #[ORM\Column(name: 'created_at', type: Types::DATETIMETZ_IMMUTABLE)]
    private \DateTimeImmutable $createdAt;

    #[ORM\Column(name: 'expires_at', type: Types::DATETIMETZ_IMMUTABLE)]
    private \DateTimeImmutable $expiresAt;

    public function __construct(string $tokenHash, Account $account, \DateTimeImmutable $createdAt, \DateTimeImmutable $expiresAt)
    {
        $this->tokenHash = $tokenHash;
        $this->account = $account;
        $this->createdAt = $createdAt;
        $this->expiresAt = $expiresAt;
    }

    public function getAccount(): Account
    {
        return $this->account;
    }

    public function getExpiresAt(): \DateTimeImmutable
    {
        return $this->expiresAt->setTimezone(new \DateTimeZone('UTC'));
    }
}
