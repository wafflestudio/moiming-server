# Moiming - 일정 관리 및 참가 신청 플랫폼

> 이벤트를 생성하고, 참가 신청을 받고, 대기열까지 관리하는 올인원 일정 관리 API 서버

## 주요 기능

- **이벤트 관리** - 이벤트 생성/수정/삭제, 정원 설정, 신청 기간 설정, 대기열 지원
- **참가 신청** - 참가 확정/대기/취소/차단 상태 관리, 게스트(비회원) 신청 지원, 대기열 자동 승격
- **인증** - 이메일/비밀번호 회원가입(이메일 인증), Google OAuth 2.0, JWT 기반 세션
- **알림** - 신청 확인/대기열 승격 등 상태 변경 시 이메일 발송
- **프로필** - 프로필 이미지 업로드 (AWS S3)

## 기술 스택

| 분류 | 기술 |
|------|------|
| Language | Kotlin 1.9 |
| Framework | Spring Boot 3.5 |
| Database | MySQL 8.4, Redis 7 |
| ORM | Spring Data JDBC |
| Auth | JWT (JJWT), BCrypt, Google OAuth 2.0 |
| Storage | AWS S3 |
| Migration | Flyway |
| Docs | SpringDoc OpenAPI (Swagger) |
| CI/CD | GitHub Actions, Docker, Docker Compose |
| Infra | EC2, Kubernetes (manifests) |

## 프로젝트 구조

```
src/main/kotlin/com/wafflestudio/spring2025/
├── config/              # 설정 (Web, DB, Redis, S3, Swagger 등)
├── common/
│   ├── exception/       # 글로벌 예외 처리
│   └── email/           # 이메일 발송 서비스
└── domain/
    ├── auth/            # 인증/인가 (로그인, 회원가입, OAuth)
    ├── user/            # 사용자 프로필 관리
    ├── event/           # 이벤트 CRUD
    └── registration/    # 참가 신청 및 대기열 관리
```

## 시작하기

### 사전 요구사항

- JDK 17+
- Docker & Docker Compose

### 로컬 실행

```bash
# 저장소 클론
git clone https://github.com/wafflestudio/23-5-team4-server.git
cd 23-5-team4-server

# 환경 변수 설정 (.env 파일 생성)
cp .env.example .env  # 필요한 값 채워 넣기

# Docker Compose로 실행
docker compose up -d
```

### 빌드

```bash
./gradlew build
```

### 테스트

```bash
./gradlew test
```

## API 문서

서버 실행 후 Swagger UI에서 확인할 수 있습니다:

```
http://localhost:8080/swagger-ui/index.html
```

## 기여 방법

[CONTRIBUTING.md](./CONTRIBUTING.md)를 참고해주세요.