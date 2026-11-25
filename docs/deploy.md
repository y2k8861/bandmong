# 배포 가이드 — AWS EC2 + RDS + Docker + GitHub Actions

포트폴리오에서 가장 흔한 구성으로 준비해 두었다. 이 저장소만으로는 서버가 생기지 않고,
**AWS 계정 만들기·결제 수단 등록·도메인 구입은 직접** 해야 한다. 그 다음부터는 아래 순서대로 하면 된다.

```
브라우저 ──https──▶ EC2 [ Caddy(인증서) ─▶ nginx(React 화면, /api 프록시) ─▶ Spring Boot ] ──▶ RDS MySQL
                          ▲
GitHub Actions ── 테스트 통과 → 이미지 GHCR에 푸시 ── (손으로) Deploy 실행 → EC2에서 pull & up
```

## 0. 이미 준비된 것

| 파일 | 하는 일 |
| --- | --- |
| `backend/Dockerfile`, `frontend/Dockerfile` | 이미지 만들기 (멀티 스테이지, 백엔드는 비루트 사용자로 실행) |
| `frontend/nginx.conf` | 화면 + `/api` 프록시, 실시간 연결(SSE) 버퍼링 끄기 |
| `docker-compose.yml` | 내 컴퓨터에서 MySQL까지 한 번에 띄우기 |
| `deploy/docker-compose.prod.yml`, `deploy/Caddyfile`, `deploy/.env.example` | EC2에서 쓰는 구성 (DB는 RDS, https는 Caddy가 자동) |
| `backend/src/main/resources/db/migration/V1__init.sql` | MySQL 테이블 (Flyway가 서버 시작 때 적용) |
| `.github/workflows/ci.yml` | 테스트 → 실제 MySQL로 마이그레이션 검증 → 전체 묶음 띄워 요청 확인 → 이미지 GHCR 푸시 |
| `.github/workflows/deploy.yml` | 손으로 실행하는 EC2 배포 + 헬스 체크 (태그로 되돌리기) |

## 1. 먼저 내 컴퓨터에서 띄워 보기

Docker Desktop만 있으면 된다.

```bash
docker compose up --build
# http://localhost:8000  → 개발용 로그인에 아무 이름 (admin 이면 관리자)
deploy/smoke-test.sh     # 다른 터미널에서: 로그인·한글·실시간 연결까지 자동 확인
```

## 2. AWS 준비 (서울 리전 ap-northeast-2)

> 비용: 2025년 7월부터 새 계정은 최대 $200 크레딧과 6개월 무료 플랜을 받는다
> ([AWS 공지](https://aws.amazon.com/about-aws/whats-new/2025/07/aws-free-tier-credits-month-free-plan)).
> 그 뒤에는 요금이 나오므로, 가입하자마자 **Billing → Budgets에서 월 예산 알림**(예: $10)을 걸어 둔다.

### 2-1. RDS (MySQL)
1. RDS → 데이터베이스 생성 → MySQL 8.4, 템플릿 "프리 티어"(또는 가장 작은 인스턴스), 스토리지 20GB.
2. DB 이름 `bandmong`, 사용자 `bandmong`, 비밀번호는 따로 적어 둔다.
3. **퍼블릭 액세스: 아니요.** 보안 그룹은 새로 만들고 나중에 EC2에서만 3306을 열어 준다.
4. 파라미터 그룹에서 `character_set_server=utf8mb4`, `collation_server=utf8mb4_0900_ai_ci` (기본값이면 그대로).

### 2-2. EC2
1. Ubuntu 24.04, 프리 티어 대상 인스턴스(메모리 1GB면 아래 스왑 필수, 여유 있으면 2GB).
2. 키 페어(.pem) 만들기 → GitHub Actions에서 접속할 때 쓴다.
3. 보안 그룹: **22(내 IP만)**, 80, 443 열기.
4. 탄력적 IP를 붙인다(재시작해도 주소가 안 바뀌게).
5. RDS 보안 그룹 인바운드에 "MySQL 3306, 소스 = EC2 보안 그룹" 추가.

```bash
# EC2에 접속해서 한 번만
sudo apt update && sudo apt install -y docker.io docker-compose-v2
sudo usermod -aG docker ubuntu && newgrp docker
# 메모리 1GB면 스왑 2GB (빌드는 GitHub에서 하므로 실행만 버티면 됨)
sudo fallocate -l 2G /swapfile && sudo chmod 600 /swapfile && sudo mkswap /swapfile && sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
mkdir -p ~/bandmong
```

### 2-3. 도메인
구입한 도메인(가비아·Route 53 등)의 A 레코드를 EC2 탄력적 IP로. Caddy가 첫 접속 때 https 인증서를 자동으로 받는다.

## 3. 서버 설정 파일 (.env)

`deploy/.env.example`을 EC2의 `~/bandmong/.env`로 복사해서 채운다. **이 파일은 저장소에 올리지 않는다.**

```bash
scp -i key.pem deploy/.env.example ubuntu@<EC2 IP>:~/bandmong/.env
ssh -i key.pem ubuntu@<EC2 IP> 'nano ~/bandmong/.env'
```

| 값 | 설명 |
| --- | --- |
| `DOMAIN` | 예: `bandmong.example.com` |
| `DB_URL` | `jdbc:mysql://<RDS 엔드포인트>:3306/bandmong` |
| `DB_USERNAME`, `DB_PASSWORD` | RDS에서 정한 값 |
| `SPRING_PROFILES_ACTIVE` | 소셜 로그인 키를 넣었으면 `prod,oauth`, 아니면 `prod` |
| `DEV_LOGIN` | 포트폴리오 데모로 누구나 둘러보게 하려면 `true` (실서비스면 `false`) |
| `ADMINS` | 관리자 계정. 개발용 로그인이면 `dev:admin`, 카카오면 `kakao:<회원번호>` |

## 4. GitHub 설정

저장소 Settings → Secrets and variables → Actions:

| 이름 | 값 |
| --- | --- |
| `EC2_HOST` | EC2 탄력적 IP |
| `EC2_USER` | `ubuntu` |
| `EC2_SSH_KEY` | .pem 파일 내용 전체 |
| `DOMAIN` | 헬스 체크용 도메인 |
| `GHCR_TOKEN` | (이미지 패키지가 비공개일 때만) `read:packages` 권한 토큰 |

이미지 패키지는 처음 푸시될 때 비공개일 수 있다. GitHub 프로필 → Packages → `bandmong-backend`/`bandmong-frontend` →
Package settings에서 Public으로 바꾸면 `GHCR_TOKEN` 없이 받을 수 있다.

## 5. 배포

1. main에 push → CI가 테스트하고 이미지를 `latest`와 커밋 sha 태그로 GHCR에 올린다.
2. Actions → **Deploy** → Run workflow (tag: `latest`).
3. 워크플로가 EC2에서 새 이미지를 받아 다시 띄우고, `https://<도메인>/actuator/health`가 UP인지 확인한다.

첫 시작 때 Flyway가 RDS에 테이블을 만들고 합주실 예시 데이터를 넣는다.

**되돌리기:** Deploy를 다시 실행하면서 tag에 이전 커밋 sha를 넣는다. 테이블을 바꾼 배포(V2 이상)를 되돌릴 때는
그 마이그레이션이 이전 코드와도 맞는지 먼저 확인한다 (그래서 컬럼은 지우기보다 "추가 → 다음 배포에서 정리" 순서로 바꾼다).

## 6. 소셜 로그인 붙이기 (선택)

각 개발자 콘솔(카카오·네이버·구글)에서 앱을 만들고 Redirect URI를 등록한다.

```
https://<도메인>/login/oauth2/code/kakao
https://<도메인>/login/oauth2/code/naver
https://<도메인>/login/oauth2/code/google
```

`.env`에 키를 넣고 `SPRING_PROFILES_ACTIVE=prod,oauth`로 바꾼 뒤 Deploy를 다시 실행한다.

## 7. 운영하면서

```bash
cd ~/bandmong
docker compose -f docker-compose.prod.yml ps
docker compose -f docker-compose.prod.yml logs -f backend   # 서버 로그
```

- RDS 자동 백업(기본 7일)을 켜 둔다.
- 세션은 서버 메모리에 있어서 배포(재시작)하면 다시 로그인해야 한다. 사용자가 늘면 Spring Session(JDBC·Redis)으로 옮긴다.
- 실시간 연결(SSE)도 서버 한 대 기준이다. 여러 대로 늘리면 Redis pub/sub이 필요하다 (docs/stage-04.md).

## 알아 둘 점: 테이블 바꾸기

- 운영 DB는 `ddl-auto: validate`라 **엔티티를 고치면 반드시 `V2__설명.sql` 같은 새 마이그레이션을 함께 추가**해야 한다.
  어긋나면 서버가 아예 뜨지 않는다(조용히 틀리는 것보다 낫다). CI의 mysql 작업이 push마다 이걸 확인한다.
- enum 값을 추가하면 MySQL enum 컬럼도 바꿔야 한다: `alter table notifications modify type enum(..., '새값') not null;`
- 이미 적용된 V1 파일은 절대 고치지 않는다(Flyway 체크섬이 달라져 서버가 뜨지 않음).
