<?php

declare(strict_types=1);

namespace App\Entity;

use Doctrine\DBAL\Types\Types;
use Doctrine\ORM\Mapping as ORM;

#[ORM\Entity]
#[ORM\Table(name: 'weight_records')]
#[ORM\Index(name: 'idx_weight_records_pet_date', columns: ['pet_id', 'date'])]
class WeightRecord extends MedicalRecord
{
    #[ORM\Column(name: 'weight_kg', type: Types::DECIMAL, precision: 8, scale: 3)]
    private string $weightKg;

    public function getWeightKg(): string
    {
        return $this->weightKg;
    }

    public function update(array $data): void
    {
        $this->updateCommon($data);
        $this->weightKg = $data['weightKg'];
    }
}
