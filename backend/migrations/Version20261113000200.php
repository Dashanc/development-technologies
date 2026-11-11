<?php

declare(strict_types=1);

namespace DoctrineMigrations;

use Doctrine\DBAL\Schema\Schema;
use Doctrine\Migrations\AbstractMigration;

final class Version20261113000200 extends AbstractMigration
{
    public function getDescription(): string
    {
        return 'Add resource revisions and account-scoped idempotency results';
    }

    public function up(Schema $schema): void
    {
        foreach (['owners', 'pets', 'weight_records', 'vaccination_records', 'vet_visit_records'] as $table) {
            $this->addSql('ALTER TABLE '.$table.' ADD revision INT NOT NULL DEFAULT 1 CHECK (revision > 0)');
        }
        $this->addSql('CREATE TABLE api_idempotency (account_id UUID NOT NULL, key_hash CHAR(64) NOT NULL, request_hash CHAR(64) NOT NULL, response_status SMALLINT NOT NULL CHECK (response_status BETWEEN 200 AND 299), response_body TEXT NOT NULL, response_etag TEXT DEFAULT NULL, expires_at TIMESTAMPTZ NOT NULL, PRIMARY KEY (account_id, key_hash))');
        $this->addSql('ALTER TABLE api_idempotency ADD CONSTRAINT fk_api_idempotency_account FOREIGN KEY (account_id) REFERENCES accounts (id) ON DELETE CASCADE NOT DEFERRABLE INITIALLY IMMEDIATE');
        $this->addSql('CREATE INDEX idx_api_idempotency_expires_at ON api_idempotency (expires_at)');
    }

    public function down(Schema $schema): void
    {
        $this->addSql('DROP TABLE api_idempotency');
        foreach (['vet_visit_records', 'vaccination_records', 'weight_records', 'pets', 'owners'] as $table) {
            $this->addSql('ALTER TABLE '.$table.' DROP revision');
        }
    }
}
