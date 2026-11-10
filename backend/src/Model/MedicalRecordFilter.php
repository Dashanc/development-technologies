<?php

declare(strict_types=1);

namespace App\Model;

final readonly class MedicalRecordFilter
{
    public function __construct(
        public ?MedicalRecordType $type = null,
        public ?\DateTimeImmutable $from = null,
        public ?\DateTimeImmutable $to = null,
    ) {
    }
}
