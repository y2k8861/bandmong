package com.bandmong.harness;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import jakarta.persistence.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 엔티티와 DB 스키마가 어긋나는 실수를 MySQL 없이 몇 초 만에 잡는다.
 * (로컬·테스트는 H2가 엔티티로 테이블을 만들어서, 마이그레이션을 빼먹어도 운영에 가서야 터진다)
 */
class SchemaRulesTest {

    private static final Path MIGRATIONS = Path.of("src/main/resources/db/migration");

    /** H2 2.x 예약어 + MySQL 8에서 자주 걸리는 예약어. 컬럼·테이블 이름으로 쓰면 SQL이 깨진다. */
    private static final Set<String> RESERVED = new HashSet<>(Arrays.asList((
            "all and any array as asymmetric authorization between both case cast check constraint cross "
            + "current_date current_time current_timestamp current_user day default distinct else end except exists "
            + "false fetch for foreign from full group groups having hour if ilike in inner intersect interval is "
            + "join key leading left like limit localtime localtimestamp minus minute month natural not null offset "
            + "on or order over partition primary qualify range regexp right row rownum rows second select set some "
            + "symmetric system_user table to top trailing true union unique unknown user using value values when "
            + "where window with year "
            + "add alter asc by call change column condition create database delete desc describe div drop dual "
            + "explain function grant index insert int interval into key keys kill lead lag lock long match mod "
            + "option outer precision procedure rank read references release rename repeat replace require "
            + "restrict return revoke schema show signal sql start trigger update usage use while write"
    ).split(" ")));

    private static List<Class<?>> entities;
    private static Map<String, String> ddlByTable;

    @BeforeAll
    static void load() throws IOException {
        entities = new ArrayList<>();
        for (JavaClass c : new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.bandmong")) {
            if (c.isAnnotatedWith(Entity.class)) entities.add(c.reflect());
        }
        ddlByTable = readMigrations();
    }

    @Test
    void 엔티티를_찾았다() {
        assertThat(entities).hasSizeGreaterThan(10);
        assertThat(ddlByTable).isNotEmpty();
    }

    @Test
    void 모든_엔티티_테이블과_컬럼이_마이그레이션에_있다() {
        List<String> missing = new ArrayList<>();
        for (Table t : tables()) {
            String ddl = ddlByTable.get(t.name);
            if (ddl == null) {
                missing.add("테이블 " + t.name + " (" + t.source + ")");
                continue;
            }
            for (String col : t.columns) {
                if (!Pattern.compile("(^|[^a-z0-9_])" + col + "($|[^a-z0-9_])").matcher(ddl).find()) {
                    missing.add("컬럼 " + t.name + "." + col + " (" + t.source + ")");
                }
            }
        }
        assertThat(missing).withFailMessage("""
                엔티티에는 있는데 Flyway 마이그레이션(src/main/resources/db/migration)에 없는 것:
                  %s
                고치는 법: 다음 번호로 새 파일을 만든다 (예: V3__설명.sql). 이미 배포된 V1, V2 파일은 고치지 않는다.
                  새 테이블 → create table ... ) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;
                  새 컬럼   → alter table <테이블> add column <컬럼> <타입> ...;
                정확한 타입은 Actions의 "Schema export"(또는 SCHEMA_EXPORT=true ./gradlew test --tests "*SchemaExportTest")가 만든 build/schema-mysql.sql 에서 복사한다.
                이걸 빼먹으면 운영(ddl-auto: validate)에서 서버가 뜨지 않는다.""",
                String.join("\n  ", missing)).isEmpty();
    }

    @Test
    void 테이블과_컬럼_이름에_SQL_예약어를_쓰지_않는다() {
        List<String> bad = new ArrayList<>();
        for (Table t : tables()) {
            if (RESERVED.contains(t.name)) bad.add("테이블 " + t.name + " (" + t.source + ")");
            for (String col : t.columns) {
                if (RESERVED.contains(col)) bad.add("컬럼 " + t.name + "." + col + " (" + t.source + ")");
            }
        }
        assertThat(bad).withFailMessage("""
                SQL 예약어를 이름으로 썼습니다 (H2 테스트나 MySQL에서 문법 오류가 납니다):
                  %s
                고치는 법: 뜻이 드러나는 이름으로 바꾼다. 예: month → due_month, user → member_user, order → sort_order.
                필드 이름은 그대로 두고 @Column(name = "due_month")만 붙여도 된다.""",
                String.join("\n  ", bad)).isEmpty();
    }

    // ── 엔티티 → 테이블·컬럼 이름 (Spring Boot 기본 이름 규칙: camelCase → snake_case) ──

    private record Table(String name, List<String> columns, String source) {}

    private static List<Table> tables() {
        List<Table> out = new ArrayList<>();
        for (Class<?> e : entities) {
            jakarta.persistence.Table ann = e.getAnnotation(jakarta.persistence.Table.class);
            String table = ann != null && !ann.name().isEmpty() ? ann.name() : snake(e.getSimpleName());
            List<String> cols = new ArrayList<>();
            for (Field f : e.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers()) || Modifier.isTransient(f.getModifiers()) || f.isSynthetic()
                        || f.isAnnotationPresent(Transient.class)
                        || f.isAnnotationPresent(OneToMany.class) || f.isAnnotationPresent(ManyToMany.class)) {
                    continue;
                }
                String where = e.getSimpleName() + "." + f.getName();
                if (f.isAnnotationPresent(ElementCollection.class)) {
                    out.add(collectionTable(e, f, where));
                    continue;
                }
                if (f.isAnnotationPresent(ManyToOne.class) || f.isAnnotationPresent(OneToOne.class)) {
                    JoinColumn jc = f.getAnnotation(JoinColumn.class);
                    cols.add(jc != null && !jc.name().isEmpty() ? jc.name() : snake(f.getName()) + "_id");
                } else {
                    cols.add(column(f));
                }
            }
            out.add(new Table(table.toLowerCase(Locale.ROOT), lower(cols), e.getSimpleName()));
        }
        return out;
    }

    private static Table collectionTable(Class<?> owner, Field f, String where) {
        CollectionTable ct = f.getAnnotation(CollectionTable.class);
        String name = ct != null && !ct.name().isEmpty() ? ct.name() : snake(owner.getSimpleName()) + "_" + snake(f.getName());
        List<String> cols = new ArrayList<>();
        if (ct != null) for (JoinColumn jc : ct.joinColumns()) if (!jc.name().isEmpty()) cols.add(jc.name());
        cols.add(column(f));
        MapKeyColumn mk = f.getAnnotation(MapKeyColumn.class);
        if (mk != null && !mk.name().isEmpty()) cols.add(mk.name());
        return new Table(name.toLowerCase(Locale.ROOT), lower(cols), where);
    }

    private static String column(Field f) {
        Column c = f.getAnnotation(Column.class);
        return c != null && !c.name().isEmpty() ? c.name() : snake(f.getName());
    }

    /** CamelCaseToUnderscoresNamingStrategy와 같은 규칙 */
    static String snake(String name) {
        StringBuilder b = new StringBuilder(name.replace('.', '_'));
        for (int i = 1; i < b.length() - 1; i++) {
            if (Character.isLowerCase(b.charAt(i - 1)) && Character.isUpperCase(b.charAt(i))
                    && Character.isLowerCase(b.charAt(i + 1))) {
                b.insert(i++, '_');
            }
        }
        return b.toString().toLowerCase(Locale.ROOT);
    }

    private static List<String> lower(List<String> cols) {
        return cols.stream().map(c -> c.replace("`", "").toLowerCase(Locale.ROOT)).toList();
    }

    // ── 마이그레이션 SQL → 테이블별 create/alter 문 ──

    private static Map<String, String> readMigrations() throws IOException {
        StringBuilder all = new StringBuilder();
        try (Stream<Path> files = Files.list(MIGRATIONS)) {
            for (Path p : files.filter(p -> p.getFileName().toString().endsWith(".sql")).sorted().toList()) {
                for (String line : Files.readAllLines(p, StandardCharsets.UTF_8)) {
                    if (!line.strip().startsWith("--")) all.append(line).append('\n');
                }
            }
        }
        Map<String, String> byTable = new HashMap<>();
        Pattern head = Pattern.compile("^\\s*(?:create\\s+table(?:\\s+if\\s+not\\s+exists)?|alter\\s+table)\\s+`?([a-z0-9_]+)`?");
        for (String stmt : all.toString().toLowerCase(Locale.ROOT).split(";")) {
            Matcher m = head.matcher(stmt);
            if (m.find()) byTable.merge(m.group(1), stmt, (a, b) -> a + "\n" + b);
        }
        return byTable;
    }
}
