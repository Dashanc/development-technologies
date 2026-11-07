#!/bin/sh
set -eu

: "${APP_SECRET:?APP_SECRET is required}"
: "${POSTGRES_DB:?POSTGRES_DB is required}"
: "${POSTGRES_USER:?POSTGRES_USER is required}"
: "${POSTGRES_PASSWORD:?POSTGRES_PASSWORD is required}"

DATABASE_URL="$(php -r '
    printf("postgresql://%s:%s@database:5432/%s?serverVersion=16&charset=utf8",
        rawurlencode(getenv("POSTGRES_USER")),
        rawurlencode(getenv("POSTGRES_PASSWORD")),
        rawurlencode(getenv("POSTGRES_DB"))
    );
')"
export DATABASE_URL

exec docker-php-entrypoint "$@"
