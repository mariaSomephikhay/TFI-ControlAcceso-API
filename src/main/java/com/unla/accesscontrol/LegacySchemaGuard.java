package com.unla.accesscontrol;

import java.sql.SQLException;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.stereotype.Component;

@Component
class LegacySchemaGuard implements HibernatePropertiesCustomizer {
    private final DataSource dataSource;

    LegacySchemaGuard(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void customize(Map<String, Object> hibernateProperties) {
        String query = """
                SELECT 1 FROM information_schema.columns
                WHERE table_schema = DATABASE() AND (
                    (table_name = 'users' AND column_name = 'user_id')
                    OR table_name IN ('user_logs', 'license_plates')
                ) LIMIT 1
                """;
        try (var connection = dataSource.getConnection();
                var statement = connection.prepareStatement(query);
                var legacy = statement.executeQuery()) {
            if (legacy.next()) {
                throw new IllegalStateException(
                        "Esquema anterior detectado. Migrá a una base nueva según docs/migration.md.");
            }
        } catch (SQLException error) {
            throw new IllegalStateException("No se pudo verificar el esquema de MySQL.", error);
        }
    }
}
