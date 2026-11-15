<?php

declare(strict_types=1);

namespace App\Exception;

final class ApiError extends \RuntimeException
{
    public function __construct(
        public readonly int $status,
        public readonly string $errorCode,
        public readonly string $messageKey,
        public readonly array $fields = [],
    ) {
        parent::__construct($messageKey);
    }
}
