package com.bandmong.harness;

import com.bandmong.app.PushService;
import com.bandmong.band.Band;
import com.bandmong.chat.ChatService;
import com.bandmong.notification.NotificationService;
import com.bandmong.stream.SseHub;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import jakarta.persistence.PrePersist;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.Repository;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * 코드 구조 규칙. 사람이든 AI든 규칙을 어기면 여기서 바로 실패하고, 실패 메시지에 고치는 법이 나온다.
 * 규칙을 바꿔야 한다면 CLAUDE.md의 "규칙"도 함께 고친다.
 */
class ArchitectureRulesTest {

    /** 밴드 운영 기능 패키지: "일어난 일"만 이벤트로 알리고, 알림·채팅 안내·실시간 전송은 직접 하지 않는다. */
    private static final String[] DOMAIN = {
            "com.bandmong.band..", "com.bandmong.jam..", "com.bandmong.gig..",
            "com.bandmong.studio..", "com.bandmong.dues.."
    };

    private static JavaClasses main;
    private static JavaClasses tests;

    @BeforeAll
    static void load() {
        main = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.bandmong");
        tests = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.ONLY_INCLUDE_TESTS)
                .importPackages("com.bandmong");
    }

    @Test
    void 기능_코드는_알림을_직접_보내지_않는다() {
        noClasses().that().resideInAnyPackage(DOMAIN)
                .should().dependOnClassesThat().belongToAnyOf(NotificationService.class, SseHub.class, PushService.class)
                .because("알림·실시간 전송은 activity/ActivityListener 한 곳에서 정한다. "
                        + "고치는 법: activity/Events 에 record를 추가해 events.publishEvent(...)로 발행하고, "
                        + "ActivityListener에 @EventListener 메서드를 만들어 거기서 notifications.notify(...)를 부른다")
                .check(main);
    }

    @Test
    void 기능_코드는_채팅_안내_메시지를_직접_쓰지_않는다() {
        noClasses().that().resideInAnyPackage(DOMAIN)
                .should().callMethod(ChatService.class, "system", Band.class, String.class)
                .because("\"○○님이 들어왔어요\" 같은 채팅 안내도 이벤트로 처리한다. "
                        + "고치는 법: 이벤트를 발행하고 ActivityListener에서 chat.system(...)을 부른다")
                .check(main);
    }

    @Test
    void 컨트롤러는_리포지토리를_직접_쓰지_않는다() {
        noClasses().that().haveSimpleNameEndingWith("Controller")
                .and().doNotHaveSimpleName("DevAuthController") // 개발용 로그인: dev 프로필에서만 켜진다
                .should().dependOnClassesThat().areAssignableTo(Repository.class)
                .because("권한 확인과 트랜잭션은 서비스에서 한다. 고치는 법: 같은 패키지의 *Service에 메서드를 만들어 호출한다")
                .check(main);
    }

    @Test
    void 공용_패키지는_기능_패키지를_모른다() {
        noClasses().that().resideInAnyPackage("com.bandmong.web..", "com.bandmong.stream..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "com.bandmong.activity..", "com.bandmong.app..", "com.bandmong.auth..", "com.bandmong.band..",
                        "com.bandmong.chat..", "com.bandmong.config..", "com.bandmong.dues..", "com.bandmong.gig..",
                        "com.bandmong.jam..", "com.bandmong.moderation..", "com.bandmong.notification..",
                        "com.bandmong.studio..", "com.bandmong.user..")
                .because("web(ApiException·ProfanityFilter 등)과 stream(SseHub)은 모든 기능이 쓰는 바닥 패키지다. "
                        + "고치는 법: 기능별 로직은 해당 기능 패키지로 옮긴다")
                .check(main);
    }

    @Test
    void 필드_주입_대신_생성자_주입() {
        noFields().should().beAnnotatedWith(Autowired.class)
                .because("final 필드 + 생성자 주입으로 테스트에서 바꿔 끼우기 쉽게 한다. 고치는 법: 생성자 파라미터로 받는다")
                .check(main);
    }

    @Test
    void 현재_시각은_Clock에서만_얻는다() {
        List<String> found = new ArrayList<>();
        for (JavaClass c : main) {
            for (JavaMethodCall call : c.getMethodCallsFromSelf()) {
                if (isWallClock(call) && !call.getOrigin().isAnnotatedWith(PrePersist.class)) {
                    found.add(call.getOrigin().getFullName() + " " + call.getSourceCodeLocation());
                }
            }
        }
        assertThat(found)
                .withFailMessage("""
                        현재 시각을 직접 읽는 코드가 있습니다:
                          %s
                        고치는 법: java.time.Clock을 생성자로 주입받아 clock.instant() / LocalDate.now(clock) 을 쓰세요.
                        엔티티라면 시각을 메서드 파라미터(Instant now)로 받습니다. (예외: @PrePersist 기본값)
                        이유: 테스트가 Clock을 고정해 "마감 지남"·"이번 주" 같은 판단을 검증하기 때문입니다.""",
                        String.join("\n  ", found))
                .isEmpty();
    }

    @Test
    void 테스트는_응답_본문을_UTF8로_읽는다() {
        List<String> found = new ArrayList<>();
        for (JavaClass c : tests) {
            for (JavaMethodCall call : c.getMethodCallsFromSelf()) {
                var t = call.getTarget();
                if (t.getOwner().isEquivalentTo(MockHttpServletResponse.class)
                        && t.getName().equals("getContentAsString") && t.getRawParameterTypes().isEmpty()) {
                    found.add(call.getOrigin().getFullName() + " " + call.getSourceCodeLocation());
                }
            }
        }
        assertThat(found)
                .withFailMessage("""
                        getContentAsString()은 ISO-8859-1로 읽어서 한글 메시지가 깨집니다:
                          %s
                        고치는 법: getContentAsString(StandardCharsets.UTF_8) 또는 ApiTestSupport.read(...)를 쓰세요.""",
                        String.join("\n  ", found))
                .isEmpty();
    }

    private static boolean isWallClock(JavaMethodCall call) {
        var t = call.getTarget();
        if (t.getOwner().isEquivalentTo(System.class) && t.getName().equals("currentTimeMillis")) return true;
        if (!t.getName().equals("now") || !t.getRawParameterTypes().isEmpty()) return false;
        for (Class<?> k : List.of(Instant.class, LocalDate.class, LocalDateTime.class, LocalTime.class,
                ZonedDateTime.class, OffsetDateTime.class)) {
            if (t.getOwner().isEquivalentTo(k)) return true;
        }
        return false;
    }
}
