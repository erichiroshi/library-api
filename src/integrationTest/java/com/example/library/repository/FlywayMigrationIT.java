package com.example.library.repository;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("it")
class FlywayMigrationIT {

	@Autowired
	DataSource dataSource;

	@Test
	void shouldHaveUserRolesTable() throws Exception {
		try (var conn = dataSource.getConnection();
				var rs = conn.getMetaData().getTables(null, "public", "tb_user_roles", null)) {

			assertTrue(rs.next());
		}
	}

	@Test
	void shouldHaveDatabaseNameTestDB() throws Exception {
		try (var conn = dataSource.getConnection()) {
			assertThat(conn.getCatalog()).isEqualTo("test");
		}
	}
}
