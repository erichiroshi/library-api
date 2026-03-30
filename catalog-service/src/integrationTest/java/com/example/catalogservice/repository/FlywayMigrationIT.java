package com.example.catalogservice.repository;

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
@DisplayName("FlywayMigrationIT - Integration Tests")
class FlywayMigrationIT {

	@Autowired
	DataSource dataSource;

	@Test
	void shouldHaveUserRolesTable() throws Exception {
		try (var conn = dataSource.getConnection();
				var rs = conn.getMetaData().getTables(null, "catalog", "tb_category", null)) {

			assertTrue(rs.next());
		}
	}

	@Test
	void shouldHaveDatabaseNameTestDB() throws Exception {
		try (var conn = dataSource.getConnection()) {
			assertThat(conn.getCatalog()).isEqualTo("testdb");
		}
	}
}
