package com.example.library.repositories;

import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest
@ActiveProfiles("it")
class FlywayMigrationIT {

    @SuppressWarnings("resource")
	@Container
    static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("library_it")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Test
    void shouldApplyAllMigrationsSuccessfully() {
        // Se o contexto subir, Flyway já rodou
        assertTrue(postgres.isRunning());
    }
    
    @Autowired
    DataSource dataSource;

    @Test
    void shouldHaveUserRolesTable() throws Exception {
        try (var conn = dataSource.getConnection();
             var rs = conn.getMetaData().getTables(null, null, "tb_user_roles", null)) {

            assertTrue(rs.next());
        }
    }

}
