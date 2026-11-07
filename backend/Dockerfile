FROM php:8.4-apache-bookworm

RUN apt-get update \
    && apt-get install -y --no-install-recommends libicu-dev libpq-dev unzip \
    && docker-php-ext-install -j"$(nproc)" intl opcache pdo_pgsql \
    && rm -rf /var/lib/apt/lists/*

COPY --from=composer:2 /usr/bin/composer /usr/local/bin/composer
COPY docker/apache.conf /etc/apache2/sites-available/000-default.conf
COPY docker/php.ini /usr/local/etc/php/conf.d/murpassport.ini
COPY --chmod=755 docker/entrypoint.sh /usr/local/bin/murpassport-entrypoint

WORKDIR /var/www/html

COPY composer.json composer.lock symfony.lock ./
RUN COMPOSER_ALLOW_SUPERUSER=1 composer install --no-dev --prefer-dist --no-interaction --no-progress --no-scripts --no-autoloader

COPY . .
RUN COMPOSER_ALLOW_SUPERUSER=1 composer dump-autoload --no-dev --classmap-authoritative --no-interaction --no-scripts \
    && mkdir -p var/cache var/log \
    && chown -R www-data:www-data var

ENV APP_ENV=prod APP_DEBUG=0

ENTRYPOINT ["murpassport-entrypoint"]
CMD ["apache2-foreground"]
