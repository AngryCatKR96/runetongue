# 작업 지시: RuneHangul 도구를 Rust로 구성하기

이 저장소(`~/my_dev/runehangul`)는 OSRS를 한국어로 번역하는 RuneLite 플러그인 RuneHangul이다. 먼저 `CLAUDE.md`가 불러오는 `AGENTS.md`(RuneLite 플러그인 규칙)와 `docs/CONTEXT.md`(배경·결정·설계·계획)를 끝까지 읽어라.

## 전제 (바꿀 수 없는 것)

- RuneLite 플러그인은 JVM에서 도는 **Java 11** 코드여야 한다. Plugin Hub는 JNI/JNA, 리플렉션, 외부 프로세스 실행, 코드 다운로드·동적 로딩을 금지하므로 **Rust 코드를 플러그인 안에서 실행할 방법은 없다.** 플러그인(`src/`, Gradle)은 Java로 남긴다.
- Plugin Hub는 이 저장소를 **Gradle로만** 빌드한다. 따라서 Rust 도구가 만든 결과물(번역 번들, 서브셋 폰트)은 `src/main/resources/` 아래에 **커밋된 파일**로 존재해야 하고, `./gradlew build`는 Rust 없이 성공해야 한다.
- 플러그인 jar는 10MiB 이하여야 한다. 번역 번들(gzip 약 4.9MB) + 폰트(0.5~2.5MB)를 넣을 계획이다.

## 목표

플러그인 바깥의 모든 도구를 Rust cargo 워크스페이스(`tools/`)로 만든다. Java 플러그인은 Rust가 만든 번들을 읽기만 한다. 둘 사이의 약속은 문서화된 데이터 형식이다.

## 할 일

1. **설계 먼저 제안하고 내 확인을 받아라.** 코드를 쓰기 전에 아래를 짧게 정리해서 보여 준다.
   - `tools/` 워크스페이스 구성(크레이트·CLI 서브커맨드 이름과 역할)
   - 번역 번들 데이터 형식 초안(`docs/DATA_FORMAT.md`로 쓸 내용): 버전 필드, 압축 방식(Java 표준 라이브러리로 풀 수 있는 gzip), 레코드 구조, 조회 키 정규화 규칙
   - 쓰려는 크레이트(예: csv, rusqlite, flate2, serde, clap, 폰트 서브셋용 크레이트 후보)와 고른 이유
   - 기존 Python 도구 중 무엇을 옮기고 무엇을 버릴지
2. **번들 생성기 (`tools`의 핵심):**
   - 입력: `~/my_dev/runelingual-ko/Runelingual-Transcripts/draft/ko/transcript_ko_*.tsv`(최종 번역, 열: english, translation, category, sub_category, source)와 영어 원문 DB `~/my_dev/runelingual-ko/Runelingual-Transcripts/updater/transcript.db`(SQLite). 이 파일들을 저장소로 복사하지 말고, 경로를 인자로 받는다. 저장소에 넣을 원본 데이터 위치는 설계 단계에서 제안한다.
   - 조회 키 정규화: 숫자 → `<Num0>`…, 색 태그 → `<colNum0>`…, 플레이어 이름 → `[player name]`, `<br>`와 공백 처리. RuneLingual의 `Transformer`, `SqlQuery`(P1~P5 조회), `DialogTranslator`를 참고하되(소스: `~/my_dev/runelingual-ko/RuneLingual-Plugin/`) 실행 중 퍼지 조회 대신 빌드 때 정규화해 둔다.
   - 성별 치환자(`[man/woman]`, `[sir/madam]`, `[he/she]` 등)를 선택지별로 펼친 영어 키를 모두 만든다.
   - 치환 기호(`<Num0>`, `<colNum0>`…`</col>`, `<br>`, `[...]`)가 원문과 번역에서 개수·철자까지 같은지 검사하고, 어긋난 줄은 빌드를 실패시킨다.
   - 출력: `src/main/resources/com/runehangul/`(정확한 경로는 설계에서) 아래 gzip 번들 + 통계(줄 수, 펼친 키 수, 크기).
3. **폰트 서브셋:** `~/my_dev/runelingual-ko/Runelingual-Transcripts/updater/fonts/ko/Galmuri11.ttf`(SIL OFL 1.1)에서 번역에 쓰인 글자(또는 한글 11,172자 전체 + ASCII + 호환 자모 — 어느 쪽일지는 내게 물어라)만 남긴 TTF를 만든다. Rust로 안정적으로 하기 어렵다면 그 이유와 대안(예: 커밋된 서브셋을 쓰고 생성은 별도 문서화)을 제안하라. OFL 라이선스 파일도 함께 둔다.
4. **번역 파이프라인 도구:** `~/my_dev/runelingual-ko/translation/`의 Python 스크립트(`prepare_batches.py`, `batch_glossary.py`, `check_batch.py`, `check_review.py`, `apply_translations.py`, `fix_particles.py`)가 하는 일을 Rust 서브커맨드로 옮긴다. LLM 호출은 하지 않는다(번역은 에이전트가 배치 파일을 읽고 쓰는 방식 그대로). `STYLE_GUIDE.md`, `glossary.tsv`, `TRANSLATE_PROMPT.md`, `REVIEW_PROMPT.md`는 저장소의 적절한 위치로 옮기자고 제안하되, 옮기기 전에 물어라.
5. **Java 쪽 최소 로더:** 번들을 백그라운드에서 읽어 메모리 맵으로 올리는 로더와 조회 함수만 만든다(클라이언트 스레드에서 IO 금지, `Filepath` 규칙 준수, 화면 반영은 아직 하지 않는다). Rust가 만든 작은 테스트 번들을 `src/test/resources`에 두고 JUnit으로 왕복 검증한다.
6. **검증:** `cargo test`, `cargo clippy`, `./gradlew build`(Rust 없이), jar 크기 확인, 번들 줄 수가 원본 tsv와 맞는지. 결과를 숫자로 보고하라.
7. `docs/CONTEXT.md`의 설계·작업 순서를 이번 구성에 맞게 고쳐라.

## 규칙

- `target/`, `build/` 같은 빌드 산출물은 커밋하지 않는다(`.gitignore` 갱신).
- 커밋은 작은 단위로, 포맷 변경과 기능 변경을 섞지 않는다. 커밋 메시지 끝에는 이 세션의 공동 작성자 표기를 붙인다.
- GitHub 원격 저장소 생성, push, PR은 하지 않는다. 필요하면 먼저 물어라.
- 게임 입력 자동화 금지. 플러그인이 게임에서 동작하는지는 내가 확인한다. 개발 클라이언트를 띄울 준비가 되면 `./gradlew run` 실행을 제안하고 무엇을 확인하면 되는지 알려 줘라.
- 판단이 갈리는 지점(번들 형식, 한글 범위, 원본 데이터 위치, 파일 이동)은 혼자 정하지 말고 물어라.
