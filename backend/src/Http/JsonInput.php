<?php

declare(strict_types=1);

namespace App\Http;

use App\Exception\ApiError;
use Symfony\Component\HttpFoundation\Request;

final class JsonInput
{
    public static function read(#[\SensitiveParameter] Request $request): array
    {
        if ($request->getContentTypeFormat() !== 'json') {
            throw new ApiError(400, 'JSON_REQUIRED', 'Send a JSON object with Content-Type: application/json.');
        }

        try {
            $data = json_decode($request->getContent(), false, 64, JSON_THROW_ON_ERROR);
        } catch (\JsonException) {
            throw new ApiError(400, 'INVALID_JSON', 'The request body must contain valid JSON.');
        }

        if (!$data instanceof \stdClass) {
            throw new ApiError(400, 'INVALID_JSON', 'The request body must be a JSON object.');
        }

        return get_object_vars($data);
    }
}
