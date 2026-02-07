<div align="center">
  <img src="https://raw.githubusercontent.com/wafflestudio/moiming-web/refs/heads/main/public/moiming-logo.png"
       alt="모이밍" width="150" />
  <br />
  <p align="center">
    <img src="https://img.shields.io/badge/Kotlin-1.9.x-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin" />
    <img src="https://img.shields.io/badge/Spring%20Boot-3.5.x-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white" alt="Spring Boot" />
    <img src="https://img.shields.io/badge/MySQL-8.x-4479A1?style=for-the-badge&logo=mysql&logoColor=white" alt="MySQL" />
    <img src="https://img.shields.io/badge/Redis-7.x-DC382D?style=for-the-badge&logo=redis&logoColor=white" alt="Redis" />
    <img src="https://img.shields.io/badge/AWS%20S3-Storage-232F3E?style=for-the-badge&logo=amazon-s3&logoColor=white" alt="AWS S3" />
  </p>
</div>

# 모이밍 서버

모이밍 서비스의 백엔드 서버입니다. Kotlin + Spring Boot 기반으로 모임, 참가, 인증, 유저, 이미지 업로드 기능을 제공합니다.

## 주요 기능

- 모임(Event) 생성 및 조회
- 참가(Registration) 신청 및 상태 변경
- 이메일/소셜 로그인, JWT 기반 인증
- 프로필/이미지 업로드용 S3 프리사인 URL 발급
- Redis 기반 캐시/토큰 관리
- Swagger(OpenAPI) 문서 제공

## 기술 스택

- Kotlin 1.9.x, Spring Boot 3.5.x
- MySQL 8.x, Redis 7.x
- Flyway (DB 마이그레이션)
- JWT, Spring Mail, Springdoc OpenAPI
- AWS S3 (이미지 업로드)

## 로컬 실행

### 사전 준비

- Java 17
- Docker (MySQL, Redis)

### 인프라 실행

```bash
docker-compose up -d
```

### 애플리케이션 실행

```bash
./gradlew bootRun
```

기본 포트는 8080입니다.

## 환경 변수

`src/main/resources/application.yaml` 기준으로 아래 값이 필요합니다.

- `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD`
- `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `GOOGLE_REDIRECT_URI`

S3 설정은 `application.yaml`의 `aws.s3` 값을 사용합니다. 필요 시 실제 운영 값으로 교체하세요.

## API 문서

Springdoc OpenAPI가 활성화되어 있습니다. 로컬 실행 후 아래 경로에서 확인할 수 있습니다.

- `/swagger-ui/index.html`

## 데이터베이스 마이그레이션

Flyway를 사용하며, SQL 파일은 다음 경로에 있습니다.

- `src/main/resources/db/migration`

## 폴더 구조

- `src/main/kotlin/com/wafflestudio/spring2025/config`: 설정 및 공통 구성
- `src/main/kotlin/com/wafflestudio/spring2025/common`: 공통 모듈
- `src/main/kotlin/com/wafflestudio/spring2025/common/email`: 이메일 발송 관련
- `src/main/kotlin/com/wafflestudio/spring2025/common/image`: 이미지 업로드/프리사인 URL
- `src/main/kotlin/com/wafflestudio/spring2025/common/exception`: 공통 예외 처리
- `src/main/kotlin/com/wafflestudio/spring2025/domain`: 도메인 모듈
- `src/main/kotlin/com/wafflestudio/spring2025/domain/auth`: 인증/인가
- `src/main/kotlin/com/wafflestudio/spring2025/domain/user`: 사용자
- `src/main/kotlin/com/wafflestudio/spring2025/domain/event`: 모임(이벤트)
- `src/main/kotlin/com/wafflestudio/spring2025/domain/registration`: 참가 신청

## 기여자

|                                                         윤찬규                                                         | 홍지수 |                                                               정성원                                                               |
|:-------------------------------------------------------------------------------------------------------------------:| :---: |:-------------------------------------------------------------------------------------------------------------------------------:|
| <a href="https://github.com/uykhc"><img src="https://github.com/uykhc.png" width="120" alt="uykhc"/></a><br/>@uykhc | <a href="https://github.com/jaylovegood"><img src="https://github.com/jaylovegood.png" width="120" alt="jaylovegood"/></a><br/>@jaylovegood | <a href="https://github.com/cjwjeong"><img src="https://github.com/cjwjeong.png" width="120" alt="cjwjeong"/></a><br/>@cjwjeong |
|                                                    events, image                                                    | auth, user |                                                      registrations, email                                                       |


## 프론트엔드

프론트엔드 저장소는 https://github.com/wafflestudio/moiming-web 에서 관리됩니다.
