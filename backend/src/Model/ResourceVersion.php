<?php

declare(strict_types=1);

namespace App\Model;

use App\Entity\MedicalRecord;
use App\Entity\Owner;
use App\Entity\Pet;
use App\Exception\ApiError;

final class ResourceVersion
{
    public static function tag(VersionedResource $resource): string
    {
        $type = match (true) {
            $resource instanceof Owner => 'owner',
            $resource instanceof Pet => 'pet',
            $resource instanceof MedicalRecord => MedicalRecordType::forRecord($resource)->value,
        };

        return $type.'.'.$resource->getId()->toRfc4122().'.'.$resource->getRevision();
    }

    public static function requireMatch(VersionedResource $resource, ?string $ifMatch): void
    {
        if ($ifMatch === null || trim($ifMatch) === '') {
            throw new ApiError(428, 'PRECONDITION_REQUIRED', 'error.precondition_required');
        }
        if (!in_array('"'.self::tag($resource).'"', array_map(trim(...), explode(',', $ifMatch)), true)) {
            throw new ApiError(412, 'VERSION_CONFLICT', 'error.version_conflict');
        }
    }
}
