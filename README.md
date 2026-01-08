# 23-5-team4-server

와플스튜디오 23.5기 4조 server

## 협업 규칙

### 작업 프로세스

1. **이슈 생성**: Github에서 이슈를 생성
2. **이슈 공유**: 슬랙에 이슈 링크 공유
3. **브랜치 생성**: 이슈와 연결된 브랜치 생성
4. **작업 완료**: 해당 이슈에 대응되는 작업 완료
5. **PR 생성**: Pull Request 생성
6. **리뷰 요청**: PR 링크를 슬랙에 공유하며 리뷰어 태그해 리뷰 요청
7. **코드 리뷰 및 병합**: 리뷰어는 코드 리뷰 후 메인에 머지

### Github Flow

- fork 없이 메인 레포를 바로 clone해서 작업 후 push
- 브랜치 전략:
  - `main`
    - `feat/name` - 새로운 기능 개발
    - `hotfix/name` - 긴급 버그 수정
    - `ref/name` - 리팩토링
- PR을 병합할 때는 스쿼시 후 병합

### 커밋 메시지

- `gitmoji` 사용 - 이모지를 커밋메시지 가장 앞에 사용
- 예시: `✨ Add a landing page`

**IntelliJ 사용자:**
- "Gitmoji Plus Commit Button" 플러그인 설치
  1. `Settings/Preferences` → `Plugins`
  2. Marketplace에서 "Gitmoji Plus Commit Button" 검색 및 설치
  3. 화면 좌측 상단 Commit 버튼 (혹은 cmd + 숫자0) -> gitmoji 플러그인 아이콘 클릭 후 이모지 선택, 커밋 메시지 작성, 커밋.
