-- Run on the new application database after its schema has been created.
CREATE TABLE IF NOT EXISTS migration_operator_map (
    legacy_id VARCHAR(255) PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    card_uid VARCHAR(64) NOT NULL UNIQUE,
    CHECK (CHAR_LENGTH(TRIM(username)) > 0),
    CHECK (CHAR_LENGTH(TRIM(card_uid)) > 0)
) ENGINE=InnoDB;

DROP PROCEDURE IF EXISTS migrate_legacy_operators;
DELIMITER //
CREATE PROCEDURE migrate_legacy_operators(IN source_database VARCHAR(64))
BEGIN
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        DROP TEMPORARY TABLE IF EXISTS source_users, source_logs, source_plates;
        RESIGNAL;
    END;

    IF source_database IS NULL OR NOT REGEXP_LIKE(source_database, '^[A-Za-z0-9_]+$')
        OR source_database = DATABASE() THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Use a separate source database with a simple name';
    END IF;

    START TRANSACTION;

    IF EXISTS (SELECT 1 FROM users) OR EXISTS (SELECT 1 FROM authentication_events)
        OR EXISTS (SELECT 1 FROM legacy_operator_ids) OR EXISTS (SELECT 1 FROM license_plates) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Migration requires empty target operator, audit and plate tables';
    END IF;

    SET @migration_query = CONCAT('CREATE TEMPORARY TABLE source_users AS ',
        'SELECT user_id, password, state, block_amount, created_at, updated_at FROM `', source_database, '`.users');
    PREPARE migration_stmt FROM @migration_query;
    EXECUTE migration_stmt;
    DEALLOCATE PREPARE migration_stmt;

    SET @migration_query = CONCAT('CREATE TEMPORARY TABLE source_logs AS ',
        'SELECT id, user_id, event, description, created_at, updated_at FROM `', source_database, '`.user_logs');
    PREPARE migration_stmt FROM @migration_query;
    EXECUTE migration_stmt;
    DEALLOCATE PREPARE migration_stmt;

    SET @migration_query = CONCAT('CREATE TEMPORARY TABLE source_plates AS ',
        'SELECT id, plate_number, created_at, updated_at FROM `', source_database, '`.license_plates');
    PREPARE migration_stmt FROM @migration_query;
    EXECUTE migration_stmt;
    DEALLOCATE PREPARE migration_stmt;

    IF EXISTS (
        SELECT 1 FROM source_users s LEFT JOIN migration_operator_map m ON m.legacy_id = s.user_id
        WHERE m.legacy_id IS NULL
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Every source operator must have exactly one reviewed mapping';
    END IF;

    IF EXISTS (
        SELECT 1 FROM migration_operator_map m LEFT JOIN source_users s ON s.user_id = m.legacy_id
        WHERE s.user_id IS NULL
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'An operator mapping references an unknown source operator';
    END IF;

    IF EXISTS (
        SELECT 1 FROM migration_operator_map m JOIN migration_operator_map other
            ON other.legacy_id = m.card_uid AND other.legacy_id <> m.legacy_id
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'A card UID collides with another operator legacy ID';
    END IF;

    IF EXISTS (
        SELECT 1 FROM source_logs l LEFT JOIN migration_operator_map m ON m.legacy_id = l.user_id
        WHERE l.user_id IS NOT NULL AND m.legacy_id IS NULL
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'An audit record references an unmapped operator';
    END IF;

    INSERT INTO users (username, card_uid, password, active, failed_attempts, created_at, updated_at)
        SELECT m.username, m.card_uid, s.password, s.state, s.block_amount, s.created_at, s.updated_at
        FROM source_users s JOIN migration_operator_map m ON m.legacy_id = s.user_id;

    INSERT INTO legacy_operator_ids (legacy_id, user_id)
        SELECT m.legacy_id, u.id FROM migration_operator_map m JOIN users u ON u.username = m.username;

    INSERT INTO authentication_events (id, user_id, event, description, created_at, updated_at)
        SELECT l.id, a.user_id, l.event, l.description, l.created_at, l.updated_at
        FROM source_logs l LEFT JOIN legacy_operator_ids a ON a.legacy_id = l.user_id;

    INSERT INTO license_plates (id, plate_number, created_at, updated_at)
        SELECT id, plate_number, created_at, updated_at FROM source_plates;

    DROP TEMPORARY TABLE source_users, source_logs, source_plates;
    COMMIT;
END//
DELIMITER ;
