package com.example.authservice.repository;

import javax.sql.DataSource;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("it")
@DisplayName("FlywayMigrationIT - Auth Service")
class FlywayMigrationIT {

    @Autowired
    DataSource dataSource;

    @Test
    @DisplayName("Schema auth deve existir")
    void shouldHaveAuthSchema() throws Exception {
        try (var conn = dataSource.getConnection();
             var rs = conn.getMetaData().getTables(null, "auth", "tb_user", null)) {
            assertTrue(rs.next());
        }
    }

    @Test
    @DisplayName("Tabela tb_refresh_tokens deve existir")
    void shouldHaveRefreshTokensTable() throws Exception {
        try (var conn = dataSource.getConnection();
             var rs = conn.getMetaData().getTables(null, "auth", "tb_refresh_tokens", null)) {
            assertTrue(rs.next());
        }
    }

    @Test
    @DisplayName("Database deve ser testdb")
    void shouldHaveDatabaseNameTestDB() throws Exception {
        try (var conn = dataSource.getConnection()) {
            assertThat(conn.getCatalog()).isEqualTo("testdb");
        }
    }
}