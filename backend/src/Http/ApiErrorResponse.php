<?php

declare(strict_types=1);

namespace App\Http;

use Symfony\Component\DependencyInjection\Attribute\Autowire;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\RequestStack;
use Symfony\Contracts\Translation\TranslatableInterface;
use Symfony\Contracts\Translation\TranslatorInterface;

final class ApiErrorResponse
{
    public function __construct(
        private readonly TranslatorInterface $translator,
        private readonly RequestStack $requests,
        #[Autowire('%kernel.enabled_locales%')] private readonly array $locales,
    ) {
    }

    public function create(int $status, string $code, string $messageKey, array $fields = []): JsonResponse
    {
        $request = $this->requests->getCurrentRequest();
        $locale = $request?->getPreferredLanguage($this->locales) ?? $this->translator->getLocale();
        $request?->setLocale($locale);
        $messages = [];
        foreach ($fields as $field => $message) {
            $messages[$field] = $message instanceof TranslatableInterface
                ? $message->trans($this->translator, $locale)
                : $this->translator->trans($message, [], 'api', $locale);
        }

        $response = ApiResponse::error($status, $code, $this->translator->trans($messageKey, [], 'api', $locale), $messages);
        $response->headers->set('Content-Language', $locale);
        $response->setVary('Accept-Language', false);

        return $response;
    }
}
