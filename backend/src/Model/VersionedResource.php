<?php

declare(strict_types=1);

namespace App\Model;

use Symfony\Component\Uid\Uuid;

interface VersionedResource
{
    public function getId(): Uuid;

    public function getRevision(): int;
}
