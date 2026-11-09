<?php

declare(strict_types=1);

namespace App\Entity;

use Doctrine\DBAL\Types\Types;
use Doctrine\ORM\Mapping as ORM;
use Symfony\Component\Uid\Uuid;

#[ORM\Entity]
#[ORM\Table(name: 'pets')]
#[ORM\Index(name: 'idx_pets_owner_id', columns: ['owner_id'])]
class Pet
{
    #[ORM\Id]
    #[ORM\Column(type: 'uuid')]
    private Uuid $id;

    #[ORM\ManyToOne(targetEntity: Owner::class)]
    #[ORM\JoinColumn(name: 'owner_id', nullable: false, onDelete: 'RESTRICT')]
    private Owner $owner;

    #[ORM\Column(type: Types::TEXT)]
    private string $name;

    #[ORM\Column(length: 16)]
    private string $species;

    #[ORM\Column(type: Types::TEXT, nullable: true)]
    private ?string $breed;

    #[ORM\Column(name: 'birth_date', type: Types::DATE_IMMUTABLE, nullable: true)]
    private ?\DateTimeImmutable $birthDate;

    #[ORM\Column(length: 16, options: ['default' => 'UNKNOWN'])]
    private string $sex;

    #[ORM\Column(type: Types::TEXT, nullable: true)]
    private ?string $coat;

    #[ORM\Column(length: 15, nullable: true)]
    private ?string $microchip;

    #[ORM\Column(name: 'passport_number', type: Types::TEXT, nullable: true)]
    private ?string $passportNumber;

    #[ORM\Column(options: ['default' => false])]
    private bool $neutered;

    #[ORM\Column(type: Types::TEXT, nullable: true)]
    private ?string $allergies;

    public function __construct(Owner $owner, array $data)
    {
        $this->id = Uuid::v7();
        $this->owner = $owner;
        $this->update($data);
    }

    public function getId(): Uuid
    {
        return $this->id;
    }

    public function getOwner(): Owner
    {
        return $this->owner;
    }

    public function getName(): string
    {
        return $this->name;
    }

    public function getSpecies(): string
    {
        return $this->species;
    }

    public function getBreed(): ?string
    {
        return $this->breed;
    }

    public function getBirthDate(): ?\DateTimeImmutable
    {
        return $this->birthDate;
    }

    public function getSex(): string
    {
        return $this->sex;
    }

    public function getExtra(): array
    {
        return [
            'coat' => $this->coat,
            'microchip' => $this->microchip,
            'passportNumber' => $this->passportNumber,
            'neutered' => $this->neutered,
            'allergies' => $this->allergies,
        ];
    }

    public function update(array $data): void
    {
        $this->name = $data['name'];
        $this->species = $data['species'];
        $this->breed = $data['breed'];
        $this->birthDate = $data['birthDate'];
        $this->sex = $data['sex'];
        $this->coat = $data['extra']['coat'];
        $this->microchip = $data['extra']['microchip'];
        $this->passportNumber = $data['extra']['passportNumber'];
        $this->neutered = $data['extra']['neutered'];
        $this->allergies = $data['extra']['allergies'];
    }
}
