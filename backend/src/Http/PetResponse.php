<?php

declare(strict_types=1);

namespace App\Http;

use App\Entity\Pet;

final class PetResponse
{
    public static function data(Pet $pet): array
    {
        return [
            'id' => $pet->getId()->toRfc4122(),
            'ownerId' => $pet->getOwner()->getId()->toRfc4122(),
            'name' => $pet->getName(),
            'species' => $pet->getSpecies(),
            'breed' => $pet->getBreed(),
            'birthDate' => $pet->getBirthDate()?->format('Y-m-d'),
            'sex' => $pet->getSex(),
            'extra' => $pet->getExtra(),
        ];
    }
}
