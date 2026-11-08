<?php

declare(strict_types=1);

namespace DoctrineMigrations;

use Doctrine\DBAL\Schema\Schema;
use Doctrine\Migrations\AbstractMigration;

final class Version20261113000100 extends AbstractMigration
{
    public function getDescription(): string
    {
        return 'Enforce normalized unique account emails and store hashed access sessions';
    }

    public function up(Schema $schema): void
    {
        $this->addSql('UPDATE accounts SET email = LOWER(BTRIM(email))');
        $this->addSql('DROP INDEX idx_accounts_email');
        $this->addSql('CREATE UNIQUE INDEX uniq_accounts_email ON accounts (email)');
        $this->addSql('ALTER TABLE accounts ADD CONSTRAINT chk_accounts_normalized_email CHECK (email = LOWER(BTRIM(email)))');
        $this->addSql('CREATE TABLE auth_sessions (token_hash CHAR(64) NOT NULL, account_id UUID NOT NULL, created_at TIMESTAMPTZ NOT NULL, expires_at TIMESTAMPTZ NOT NULL, PRIMARY KEY(token_hash), CONSTRAINT chk_auth_sessions_expiry CHECK (expires_at > created_at))');
        $this->addSql('CREATE INDEX idx_auth_sessions_account ON auth_sessions (account_id)');
        $this->addSql('CREATE INDEX idx_auth_sessions_expiry ON auth_sessions (expires_at)');
        $this->addSql('ALTER TABLE auth_sessions ADD CONSTRAINT fk_auth_sessions_account FOREIGN KEY (account_id) REFERENCES accounts (id) ON DELETE CASCADE NOT DEFERRABLE INITIALLY IMMEDIATE');
    }

    public function down(Schema $schema): void
    {
        $this->addSql('DROP TABLE auth_sessions');
        $this->addSql('ALTER TABLE accounts DROP CONSTRAINT chk_accounts_normalized_email');
        $this->addSql('DROP INDEX uniq_accounts_email');
        $this->addSql('CREATE INDEX idx_accounts_email ON accounts (email)');
    }
}
