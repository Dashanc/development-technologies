<?php

declare(strict_types=1);

namespace App\Model;

use App\Entity\Owner;

final readonly class AuthenticationResult
{
    public function __construct(public Owner $owner, public IssuedSession $credentials)
    {
    }
}
