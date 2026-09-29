-- Ejecutar una sola vez, con la API detenida, sobre la base gestionusuario
-- creada por la version que ya usa patentes, users y user_logs.
-- Las fechas historicas de patentes y usuarios no pueden recuperarse:
-- para filas existentes se toma la fecha de esta migracion.

ALTER TABLE `patentes`
    DROP PRIMARY KEY,
    ADD COLUMN `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY FIRST,
    ADD UNIQUE KEY `uk_patentes_numero` (`numero`),
    ADD COLUMN `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    ADD COLUMN `updated_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6);

ALTER TABLE `users`
    ADD COLUMN `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    ADD COLUMN `updated_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6);

ALTER TABLE `user_logs`
    ADD COLUMN `updated_at` DATETIME(6) NULL;

UPDATE `user_logs` SET `updated_at` = `created_at`;

ALTER TABLE `user_logs`
    MODIFY COLUMN `updated_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6);
