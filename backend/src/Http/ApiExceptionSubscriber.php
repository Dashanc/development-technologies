<?php

declare(strict_types=1);

namespace App\Http;

use App\Exception\ApiError;
use Psr\Log\LoggerInterface;
use Symfony\Component\EventDispatcher\EventSubscriberInterface;
use Symfony\Component\HttpKernel\Event\ExceptionEvent;
use Symfony\Component\HttpKernel\Exception\HttpExceptionInterface;
use Symfony\Component\HttpKernel\KernelEvents;
use Symfony\Component\Security\Core\Exception\AccessDeniedException;
use Symfony\Component\Security\Core\Exception\AuthenticationException;

final class ApiExceptionSubscriber implements EventSubscriberInterface
{
    public function __construct(private readonly LoggerInterface $logger, private readonly ApiErrorResponse $responses)
    {
    }

    public static function getSubscribedEvents(): array
    {
        return [KernelEvents::EXCEPTION => ['onException', 256]];
    }

    public function onException(ExceptionEvent $event): void
    {
        if (!str_starts_with($event->getRequest()->getPathInfo(), '/api/')) {
            return;
        }

        $exception = $event->getThrowable();
        if ($exception instanceof AuthenticationException || $exception instanceof AccessDeniedException) {
            return;
        }
        if ($exception instanceof ApiError) {
            $response = $this->responses->create($exception->status, $exception->errorCode, $exception->messageKey, $exception->fields);
        } elseif ($exception instanceof HttpExceptionInterface) {
            $status = $exception->getStatusCode();
            $message = match ($status) {
                404 => 'error.not_found',
                405 => 'error.method_not_allowed',
                default => 'error.request_failed',
            };
            $response = $this->responses->create($status, 'HTTP_'.$status, $message);
            foreach (['Allow', 'WWW-Authenticate', 'Retry-After'] as $header) {
                if (isset($exception->getHeaders()[$header])) {
                    $response->headers->set($header, $exception->getHeaders()[$header]);
                }
            }
        } else {
            $this->logger->error('API request failed ({exception_class} at {exception_file}:{exception_line}).', [
                'exception_class' => $exception::class,
                'exception_file' => $exception->getFile(),
                'exception_line' => $exception->getLine(),
            ]);
            $response = $this->responses->create(500, 'INTERNAL_ERROR', 'error.internal_error');
        }

        $event->setResponse($response);
    }
}
