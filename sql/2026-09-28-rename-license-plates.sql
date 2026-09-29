-- Ejecutar una sola vez, con la API detenida, despues de la migracion
-- 2026-09-28-ids-y-auditoria.sql si la base ya existia.
-- Conserva los registros y los ID autogenerados.

RENAME TABLE `patentes` TO `license_plates`;

ALTER TABLE `license_plates`
    CHANGE COLUMN `numero` `plate_number` VARCHAR(16) NOT NULL,
    RENAME INDEX `uk_patentes_numero` TO `uk_license_plates_number`;
