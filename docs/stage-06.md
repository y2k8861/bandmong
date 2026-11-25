# 6단계 — 신고·운영, 배포 준비

## 6-1. 신고·운영

| 기능 | API | 화면 |
| --- | --- | --- |
| 신고 (회원·밴드·메시지) | `POST /api/reports` `{targetType, targetId, reason, detail}` | 밴드 상세(밴드 신고, 멤버 ⋯), 채팅(남의 메시지 누르기) |
| 신고 목록 (관리자) | `GET /api/admin/reports?status=OPEN` | /admin |
| 신고 처리 (관리자) | `POST /api/admin/reports/{id}/resolve` `{action, note}` | /admin |
| 이용 제한 풀기 (관리자) | `POST /api/admin/users/{id}/lift` | /admin |
| 방장 넘기기 | `POST /api/bands/{id}/host` `{userId}` | 밴드 상세 → 멤버 ⋯ |
| 탈퇴 | `DELETE /api/me` | 마이 |

처리 방법: 문제없음 · 경고 · 7일 제한 · 30일 제한 · 메시지 가리기(메시지 신고) · 모집 닫기(밴드 신고).

### 왜 이렇게 만들었나

**관리자는 설정으로만 정한다.** `bandmong.admins`(`provider:providerId` 목록)에 적힌 계정이 로그인할 때 ADMIN이 된다.
API로 관리자를 만들 수 있으면 그 API가 가장 위험한 구멍이 된다. `/api/admin/**`은 Spring Security에서 `hasRole("ADMIN")`.

**이용 제한은 "보기만".** 제한된 계정을 아예 막으면 왜 막혔는지도 볼 수 없다. `AccountGuard`(인터셉터)가 모든 API 앞에서
쓰기 요청(POST·PUT·PATCH·DELETE)만 403 "○월 ○일까지 이용이 제한돼요"로 막고, 조회와 탈퇴·로그아웃은 둔다.
탈퇴한 계정의 남은 세션(다른 기기)도 여기서 401로 끊는다.

**같은 대상 신고는 한 번에.** 같은 메시지를 다섯 명이 신고하면 관리자는 한 번 처리하고, 다섯 신고가 같은 결과로 닫히고
다섯 명 모두 결과 알림을 받는다. 관리자 화면에는 "이 대상 신고 N건, 이 사람 누적 N건"을 함께 보여 판단을 돕는다.

**남용 막기.** 하루 10건, 같은 대상에 열린 신고는 한 번, 나·내 밴드·내 메시지는 신고 불가, 메시지는 그 밴드 멤버만 신고 가능
(볼 수 없는 메시지를 id로 찍어 신고하지 못하게). 신고자는 상대에게 알리지 않는다.

**지우지 않고 가린다.** 메시지 가리기는 내용 대신 "운영 정책에 따라 가려진 메시지예요"를 보여 주고 원문은 남긴다
(재신고·이의 제기 때 근거). 제재도 기록(`sanctions`)으로 남기고, 일찍 풀면 `lifted_at`만 채운다.

**탈퇴는 익명화.** 닉네임·이메일·지역·악기를 지우고 소셜 계정 연결(`provider_id`)을 끊는다. 같은 카카오 계정으로 오면 새 회원.
채팅·합주 기록은 남되 "탈퇴한 사용자"로 보인다. 다른 멤버가 있는 밴드의 방장이면 먼저 방장을 넘겨야 한다
(방장 없는 밴드가 생기지 않게). 그래서 방장 넘기기를 같이 만들었다.

## 6-2. 배포 준비

포트폴리오에서 흔한 **AWS EC2 + RDS(MySQL) + Docker + GitHub Actions** 구성. 실제 서버는 AWS 계정이 필요해서 만들지 않았고,
계정만 있으면 바로 올릴 수 있게 준비했다 → [배포 가이드](deploy.md).

```
push → CI ┬ backend 테스트 (H2)
          ├ mysql: MySQL 8.4에 prod 설정으로 붙여 Flyway + validate + 주요 흐름
          ├ frontend 테스트·빌드
          ├ stack: docker compose로 nginx→Spring→MySQL 띄우고 실제 요청 (deploy/smoke-test.sh)
          └ images: GHCR에 latest + 커밋 sha 푸시
(손으로) Deploy → EC2에서 새 이미지 pull & up → https 헬스 체크
```

### 왜 이렇게 만들었나

**테이블은 Flyway만 바꾼다.** 지금까지는 엔티티를 고치면 Hibernate가 테이블을 알아서 바꿨다(`ddl-auto: update`).
운영에서 그러면 컬럼 이름 하나 바꿨다가 데이터가 사라질 수 있다. 운영은 `validate` — 엔티티와 테이블이 어긋나면 서버가 아예 뜨지 않는다.
첫 마이그레이션(`V1__init.sql`)은 엔티티에서 Hibernate로 MySQL용 CREATE 문을 뽑아(`schema.yml` 워크플로) 정리했다.

**마이그레이션은 진짜 MySQL로 검증.** 로컬 테스트는 빠른 H2(MySQL 모드)지만, H2와 MySQL은 미묘하게 다르다
(예: `month`는 H2 예약어라 5단계에서 한 번 깨졌다). CI의 mysql 작업이 push마다 MySQL 8.4 컨테이너에 운영 설정 그대로 붙여
Flyway 적용 → validate → 이모지 저장·UTC 시간 왕복·중복 방지까지 확인한다.

**전체 묶음도 CI에서 띄운다.** Dockerfile·nginx 설정은 단위 테스트로 못 잡는다. stack 작업이 `docker compose up`으로
nginx → Spring → MySQL을 띄우고 실제 HTTP로 CSRF 쿠키 → 로그인 → 한글 저장 → 실시간 연결이 nginx를 버퍼링 없이 지나는지 본다.

**한 주소로 화면과 API.** nginx가 React 파일을 내보내면서 `/api`를 백엔드로 넘긴다. 같은 출처라서 1단계의 세션·CSRF 쿠키가
그대로 동작하고 CORS 설정이 필요 없다. SSE(`/api/stream`)만 버퍼링을 끄고 읽기 시간을 1시간으로 둔다.

**https는 Caddy.** 도메인만 정해 주면 Let's Encrypt 인증서를 받아 오고 갱신까지 한다. 앞단이 붙인 `X-Forwarded-Proto`를
nginx가 넘기고 Spring이 믿으므로(`forward-headers-strategy`), 쿠키 `Secure`와 소셜 로그인 리다이렉트 주소가 https로 맞는다.

**이미지 태그 = 커밋.** `latest`와 함께 커밋 sha로도 올려서, 배포가 잘못되면 Deploy를 이전 sha로 다시 실행해 되돌린다.

**설정은 모두 환경변수.** DB 비밀번호·소셜 키는 저장소에 없다. 소셜 로그인은 `oauth` 프로필로 분리해서, 키가 없어도 운영
프로필로 띄울 수 있다(포트폴리오 데모 서버는 개발용 로그인을 켜서 누구나 둘러보게 할 수 있음).

## 테스트

- `ModerationApiTest`: 메시지 신고 중복·자기 것·밴드 밖 사람, 관리자만 목록, 같은 대상 2건 한 번에 가리기·알림,
  7일 제한 중 쓰기 403·조회 OK·`restrictedUntil`·제한 풀기, 이미 처리된 신고 409, 방장 넘기기, 탈퇴(방장 막힘·익명화·세션 끊김·같은 이름 새 계정).
- `MySqlMigrationTest`(CI mysql 작업), `deploy/smoke-test.sh`(CI stack 작업).
- 프론트: 관리자 처리 후 같은 대상 함께 빠짐, 신고 시트.

## 남은 일

- 실제 AWS에 올리기 (계정 준비 후 docs/deploy.md 순서대로)
- 세션을 DB·Redis로 (재배포해도 로그인 유지), 서버 여러 대일 때 SSE용 Redis pub/sub
- 제재 이의 제기, 관리자 활동 기록 화면
