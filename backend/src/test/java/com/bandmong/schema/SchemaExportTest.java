package com.bandmong.schema;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 엔티티에서 MySQL용 CREATE 문을 뽑아 build/schema-mysql.sql 로 쓴다.
 * Flyway 마이그레이션 초안을 만들 때만 쓴다: SCHEMA_EXPORT=true ./gradlew test --tests '*SchemaExportTest'
 */
@EnabledIfEnvironmentVariable(named = "SCHEMA_EXPORT", matches = "true")
@SpringBootTest(properties = {
        "spring.jpa.properties.jakarta.persistence.schema-generation.scripts.action=create",
        "spring.jpa.properties.jakarta.persistence.schema-generation.scripts.create-target=build/schema-mysql.sql",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect",
        "spring.jpa.properties.hibernate.hbm2ddl.delimiter=;",
        "spring.jpa.properties.hibernate.format_sql=true"
})
class SchemaExportTest {
    @Test
    void export() {
        // 컨텍스트가 뜨면서 파일이 써진다
    }
}
