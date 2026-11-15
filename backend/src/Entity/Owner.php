<?php

declare(strict_types=1);

namespace App\Entity;

use App\Model\VersionedResource;
use Doctrine\DBAL\Types\Types;
use Doctrine\ORM\Mapping as ORM;
use Symfony\Component\Uid\Uuid;

#[ORM\Entity]
#[ORM\Table(name: 'owners')]
class Owner implements VersionedResource
{
    #[ORM\Column(options: ['default' => 1])]
    private int $revision = 1;

    #[ORM\Id]
    #[ORM\Column(type: 'uuid')]
    private Uuid $id;

    #[ORM\OneToOne(targetEntity: Account::class)]
    #[ORM\JoinColumn(name: 'account_id', nullable: false, onDelete: 'RESTRICT')]
    private Account $account;

    #[ORM\Column(type: Types::TEXT)]
    private string $name;

    #[ORM\Column(type: Types::TEXT, nullable: true)]
    private ?string $phone;

    public function __construct(Account $account, string $name, ?string $phone, ?Uuid $id = null)
    {
        $this->id = $id ?? Uuid::v7();
        $this->account = $account;
        $this->name = $name;
        $this->phone = $phone;
    }

    public function getId(): Uuid
    {
        return $this->id;
    }

    public function getRevision(): int
    {
        return $this->revision;
    }

    public function getAccount(): Account
    {
        return $this->account;
    }

    public function getName(): string
    {
        return $this->name;
    }

    public function getPhone(): ?string
    {
        return $this->phone;
    }

    public function update(string $name, ?string $phone): void
    {
        ++$this->revision;
        $this->name = $name;
        $this->phone = $phone;
    }
}
