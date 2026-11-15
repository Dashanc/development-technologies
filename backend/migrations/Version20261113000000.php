<?php

declare(strict_types=1);

namespace DoctrineMigrations;

use Doctrine\DBAL\Schema\Schema;
use Doctrine\Migrations\AbstractMigration;

final class Version20261113000000 extends AbstractMigration
{
    public function getDescription(): string
    {
        return 'Create account, owner, pet, and medical record tables';
    }

    public function up(Schema $schema): void
    {
        $this->addSql('CREATE TABLE accounts (id UUID NOT NULL, email VARCHAR(254) NOT NULL, password_hash VARCHAR(255) NOT NULL, created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY(id))');
        $this->addSql('CREATE INDEX idx_accounts_email ON accounts (email)');

        $this->addSql('CREATE TABLE owners (id UUID NOT NULL, account_id UUID NOT NULL, name TEXT NOT NULL, phone TEXT DEFAULT NULL, PRIMARY KEY(id))');
        $this->addSql('CREATE UNIQUE INDEX uniq_owners_account_id ON owners (account_id)');
        $this->addSql('ALTER TABLE owners ADD CONSTRAINT fk_owners_account FOREIGN KEY (account_id) REFERENCES accounts (id) ON DELETE RESTRICT NOT DEFERRABLE INITIALLY IMMEDIATE');

        $this->addSql("CREATE TABLE pets (id UUID NOT NULL, owner_id UUID NOT NULL, name TEXT NOT NULL, species VARCHAR(16) NOT NULL, breed TEXT DEFAULT NULL, birth_date DATE DEFAULT NULL, sex VARCHAR(16) NOT NULL DEFAULT 'UNKNOWN', coat TEXT DEFAULT NULL, microchip VARCHAR(15) DEFAULT NULL, passport_number TEXT DEFAULT NULL, neutered BOOLEAN NOT NULL DEFAULT FALSE, allergies TEXT DEFAULT NULL, PRIMARY KEY(id), CONSTRAINT chk_pets_species CHECK (species IN ('CAT', 'DOG', 'OTHER')), CONSTRAINT chk_pets_sex CHECK (sex IN ('FEMALE', 'MALE', 'UNKNOWN')), CONSTRAINT chk_pets_microchip CHECK (microchip IS NULL OR microchip ~ '^[0-9]{15}$'))");
        $this->addSql('CREATE INDEX idx_pets_owner_id ON pets (owner_id)');
        $this->addSql('ALTER TABLE pets ADD CONSTRAINT fk_pets_owner FOREIGN KEY (owner_id) REFERENCES owners (id) ON DELETE RESTRICT NOT DEFERRABLE INITIALLY IMMEDIATE');

        $this->addSql('CREATE TABLE weight_records (id UUID NOT NULL, pet_id UUID NOT NULL, date DATE NOT NULL, weight_kg NUMERIC(8, 3) NOT NULL, note TEXT DEFAULT NULL, PRIMARY KEY(id), CONSTRAINT chk_weight_records_positive_weight CHECK (weight_kg > 0))');
        $this->addSql('CREATE INDEX idx_weight_records_pet_date ON weight_records (pet_id, date)');
        $this->addSql('ALTER TABLE weight_records ADD CONSTRAINT fk_weight_records_pet FOREIGN KEY (pet_id) REFERENCES pets (id) ON DELETE CASCADE NOT DEFERRABLE INITIALLY IMMEDIATE');

        $this->addSql('CREATE TABLE vaccination_records (id UUID NOT NULL, pet_id UUID NOT NULL, date DATE NOT NULL, vaccine TEXT NOT NULL, next_vaccination_date DATE DEFAULT NULL, clinic TEXT DEFAULT NULL, veterinarian TEXT DEFAULT NULL, note TEXT DEFAULT NULL, PRIMARY KEY(id), CONSTRAINT chk_vaccination_records_next_date CHECK (next_vaccination_date IS NULL OR next_vaccination_date > date))');
        $this->addSql('CREATE INDEX idx_vaccination_records_pet_date ON vaccination_records (pet_id, date)');
        $this->addSql('ALTER TABLE vaccination_records ADD CONSTRAINT fk_vaccination_records_pet FOREIGN KEY (pet_id) REFERENCES pets (id) ON DELETE CASCADE NOT DEFERRABLE INITIALLY IMMEDIATE');

        $this->addSql('CREATE TABLE vet_visit_records (id UUID NOT NULL, pet_id UUID NOT NULL, date DATE NOT NULL, reason TEXT NOT NULL, clinic TEXT DEFAULT NULL, veterinarian TEXT DEFAULT NULL, diagnosis TEXT DEFAULT NULL, recommendations TEXT DEFAULT NULL, appointment_date DATE DEFAULT NULL, note TEXT DEFAULT NULL, PRIMARY KEY(id))');
        $this->addSql('CREATE INDEX idx_vet_visit_records_pet_date ON vet_visit_records (pet_id, date)');
        $this->addSql('ALTER TABLE vet_visit_records ADD CONSTRAINT fk_vet_visit_records_pet FOREIGN KEY (pet_id) REFERENCES pets (id) ON DELETE CASCADE NOT DEFERRABLE INITIALLY IMMEDIATE');
    }

    public function down(Schema $schema): void
    {
        $this->addSql('ALTER TABLE vet_visit_records DROP CONSTRAINT fk_vet_visit_records_pet');
        $this->addSql('ALTER TABLE vaccination_records DROP CONSTRAINT fk_vaccination_records_pet');
        $this->addSql('ALTER TABLE weight_records DROP CONSTRAINT fk_weight_records_pet');
        $this->addSql('ALTER TABLE pets DROP CONSTRAINT fk_pets_owner');
        $this->addSql('ALTER TABLE owners DROP CONSTRAINT fk_owners_account');
        $this->addSql('DROP TABLE vet_visit_records');
        $this->addSql('DROP TABLE vaccination_records');
        $this->addSql('DROP TABLE weight_records');
        $this->addSql('DROP TABLE pets');
        $this->addSql('DROP TABLE owners');
        $this->addSql('DROP TABLE accounts');
    }
}
