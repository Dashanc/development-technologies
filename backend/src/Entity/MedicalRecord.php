<?php

declare(strict_types=1);

namespace App\Entity;

use Doctrine\DBAL\Types\Types;
use Doctrine\ORM\Mapping as ORM;
use Symfony\Component\Uid\Uuid;

#[ORM\MappedSuperclass]
abstract class MedicalRecord
{
    #[ORM\Id]
    #[ORM\Column(type: 'uuid')]
    private Uuid $id;

    #[ORM\ManyToOne(targetEntity: Pet::class)]
    #[ORM\JoinColumn(name: 'pet_id', nullable: false, onDelete: 'CASCADE')]
    private Pet $pet;

    #[ORM\Column(type: Types::DATE_IMMUTABLE)]
    private \DateTimeImmutable $date;

    #[ORM\Column(type: Types::TEXT, nullable: true)]
    private ?string $note;

    public function __construct(Pet $pet, array $data)
    {
        $this->id = Uuid::v7();
        $this->pet = $pet;
        $this->update($data);
    }

    public function getId(): Uuid
    {
        return $this->id;
    }

    public function getPet(): Pet
    {
        return $this->pet;
    }

    public function getDate(): \DateTimeImmutable
    {
        return $this->date;
    }

    public function getNote(): ?string
    {
        return $this->note;
    }

    protected function updateCommon(array $data): void
    {
        $this->date = $data['date'];
        $this->note = $data['note'];
    }

    abstract public function update(array $data): void;
}
