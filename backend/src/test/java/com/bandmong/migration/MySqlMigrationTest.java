package com.bandmong.migration;

import com.bandmong.support.ApiTestSupport;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 운영 설정(prod 프로필) 그대로 실제 MySQL에 붙여 본다. CI의 mysql 작업에서만 돈다.
 * - Flyway가 V1부터 테이블을 만들고
 * - Hibernate validate가 엔티티와 테이블이 맞는지 확인하고 (어긋나면 여기서 컨텍스트가 안 뜸)
 * - 이모지·한글 저장, 시간(UTC) 왕복, 유니크 키가 MySQL에서도 그대로 동작하는지 본다.
 */
@EnabledIfEnvironmentVariable(named = "DB_URL", matches = "jdbc:mysql:.*")
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("prod")
class MySqlMigrationTest extends ApiTestSupport {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void 마이그레이션과_엔티티가_맞고_주요_흐름이_MySQL에서_돈다() throws Exception {
        Integer applied = jdbc.queryForObject(
                "select count(*) from flyway_schema_history where success = 1", Integer.class);
        assertThat(applied).isGreaterThanOrEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from studios", Integer.class)).isEqualTo(7);

        Person host = person("마이방장", "[\"GUITAR\"]");
        Person bass = person("마이베이스", "[\"BASS\"]");
        long bandId = bandWithMember(host, bass, "MySQL 밴드");

        JsonNode msg = read(expect(post_(bass, "/api/bands/" + bandId + "/messages",
                "{\"body\":\"이모지도 되나요 🎸🥁\",\"clientId\":\"mysql-0001\"}"), 200));
        assertThat(msg.get("body").asText()).isEqualTo("이모지도 되나요 🎸🥁");
        JsonNode again = read(expect(post_(bass, "/api/bands/" + bandId + "/messages",
                "{\"body\":\"이모지도 되나요 🎸🥁\",\"clientId\":\"mysql-0001\"}"), 200));
        assertThat(again.get("id").asLong()).isEqualTo(msg.get("id").asLong());

        String start = java.time.LocalDate.now(java.time.ZoneId.of("Asia/Seoul")).plusDays(3)
                .atTime(20, 0).atZone(java.time.ZoneId.of("Asia/Seoul")).toOffsetDateTime().toString();
        JsonNode jam = read(expect(post_(host, "/api/bands/" + bandId + "/jams",
                "{\"startsAt\":\"" + start + "\",\"durationMinutes\":120}"), 201));
        // UTC로 저장했다가 읽어도 같은 순간이어야 한다
        assertThat(java.time.Instant.parse(jam.get("startsAt").asText()))
                .isEqualTo(java.time.OffsetDateTime.parse(start).toInstant());

        JsonNode notis = read(get_(bass, "/api/notifications")).get("items");
        assertThat(notis.get(0).get("type").asText()).isEqualTo("JAM_SCHEDULED");
    }
}
