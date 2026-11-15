<?php

declare(strict_types=1);

namespace App\Entity;

use Doctrine\DBAL\Types\Types;
use Doctrine\ORM\Mapping as ORM;

#[ORM\Entity]
#[ORM\Table(name: 'vaccination_records')]
#[ORM\Index(name: 'idx_vaccination_records_pet_date', columns: ['pet_id', 'date'])]
class VaccinationRecord extends MedicalRecord
{
    #[ORM\Column(type: Types::TEXT)]
    private string $vaccine;

    #[ORM\Column(name: 'next_vaccination_date', type: Types::DATE_IMMUTABLE, nullable: true)]
    private ?\DateTimeImmutable $nextVaccinationDate;

    #[ORM\Column(type: Types::TEXT, nullable: true)]
    private ?string $clinic;

    #[ORM\Column(type: Types::TEXT, nullable: true)]
    private ?string $veterinarian;

    public function getVaccine(): string
    {
        return $this->vaccine;
    }

    public function getNextVaccinationDate(): ?\DateTimeImmutable
    {
        return $this->nextVaccinationDate;
    }

    public function getClinic(): ?string
    {
        return $this->clinic;
    }

    public function getVeterinarian(): ?string
    {
        return $this->veterinarian;
    }

    public function update(array $data): void
    {
        $this->updateCommon($data);
        $this->vaccine = $data['vaccine'];
        $this->nextVaccinationDate = $data['nextVaccinationDate'];
        $this->clinic = $data['clinic'];
        $this->veterinarian = $data['veterinarian'];
    }
}
