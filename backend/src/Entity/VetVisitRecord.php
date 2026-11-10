<?php

declare(strict_types=1);

namespace App\Entity;

use Doctrine\DBAL\Types\Types;
use Doctrine\ORM\Mapping as ORM;

#[ORM\Entity]
#[ORM\Table(name: 'vet_visit_records')]
#[ORM\Index(name: 'idx_vet_visit_records_pet_date', columns: ['pet_id', 'date'])]
class VetVisitRecord extends MedicalRecord
{
    #[ORM\Column(type: Types::TEXT)]
    private string $reason;

    #[ORM\Column(type: Types::TEXT, nullable: true)]
    private ?string $clinic;

    #[ORM\Column(type: Types::TEXT, nullable: true)]
    private ?string $veterinarian;

    #[ORM\Column(type: Types::TEXT, nullable: true)]
    private ?string $diagnosis;

    #[ORM\Column(type: Types::TEXT, nullable: true)]
    private ?string $recommendations;

    #[ORM\Column(name: 'appointment_date', type: Types::DATE_IMMUTABLE, nullable: true)]
    private ?\DateTimeImmutable $appointmentDate;

    public function getReason(): string
    {
        return $this->reason;
    }

    public function getClinic(): ?string
    {
        return $this->clinic;
    }

    public function getVeterinarian(): ?string
    {
        return $this->veterinarian;
    }

    public function getDiagnosis(): ?string
    {
        return $this->diagnosis;
    }

    public function getRecommendations(): ?string
    {
        return $this->recommendations;
    }

    public function getAppointmentDate(): ?\DateTimeImmutable
    {
        return $this->appointmentDate;
    }

    public function update(array $data): void
    {
        $this->updateCommon($data);
        $this->reason = $data['reason'];
        $this->clinic = $data['clinic'];
        $this->veterinarian = $data['veterinarian'];
        $this->diagnosis = $data['diagnosis'];
        $this->recommendations = $data['recommendations'];
        $this->appointmentDate = $data['appointmentDate'];
    }
}
