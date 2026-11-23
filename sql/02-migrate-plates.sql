-- Run against the destination from stage 1, with the API stopped.
CREATE TABLE IF NOT EXISTS migration_plate_owner_map (
    plate_id BIGINT PRIMARY KEY,
    person_id BIGINT NOT NULL,
    FOREIGN KEY (person_id) REFERENCES people (id)
) ENGINE=InnoDB;

DROP PROCEDURE IF EXISTS migrate_legacy_plates;
DELIMITER //
CREATE PROCEDURE migrate_legacy_plates()
BEGIN
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'card_uid'
    ) OR EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = DATABASE() AND table_name = 'legacy_license_plates'
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Run stage 2 once on the migrated operator database';
    END IF;

    START TRANSACTION;

    IF EXISTS (
        SELECT 1 FROM license_plates l LEFT JOIN migration_plate_owner_map m ON m.plate_id = l.id
        WHERE m.plate_id IS NULL
    ) OR EXISTS (
        SELECT 1 FROM migration_plate_owner_map m LEFT JOIN license_plates l ON l.id = m.plate_id
        WHERE l.id IS NULL
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Every source plate must have exactly one reviewed owner mapping';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM cars) THEN
        INSERT INTO cars (id, license_plate, person_id, created_at, updated_at)
            SELECT l.id, l.plate_number, m.person_id, l.created_at, l.updated_at
            FROM license_plates l JOIN migration_plate_owner_map m ON m.plate_id = l.id;
    END IF;

    IF (SELECT COUNT(*) FROM cars) <> (SELECT COUNT(*) FROM license_plates) OR EXISTS (
        SELECT 1 FROM license_plates l
        JOIN migration_plate_owner_map m ON m.plate_id = l.id
        LEFT JOIN cars c ON c.id = l.id
        WHERE c.id IS NULL OR BINARY c.license_plate <> BINARY l.plate_number
            OR c.person_id <> m.person_id OR c.created_at <> l.created_at OR c.updated_at <> l.updated_at
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Target cars do not match source plates and owners';
    END IF;

    COMMIT;

    -- Keep a copy for review and rollback. A retry checks the copied rows before renaming.
    RENAME TABLE license_plates TO legacy_license_plates;
END//
DELIMITER ;
