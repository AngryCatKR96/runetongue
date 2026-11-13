# RuneHangul 프로젝트 컨텍스트

Old School RuneScape(OSRS)를 한국어로 플레이하게 해 주는 RuneLite 플러그인. 2026-11 시작.
이 문서는 프로젝트를 시작하게 된 배경, 확정된 결정, 이전 작업(RuneLingual 한국어 기여)에서 배운 것, 설계 방향을 모아 둔다. 새로 작업을 시작하는 사람이나 에이전트는 이 문서와 루트의 `AGENTS.md`(RuneLite 공식 플러그인 개발 규칙)를 먼저 읽는다.

## 1. 배경

- 2026-10, 기존 번역 플러그인 **RuneLingual**(Plugin Hub 버전: YS-jack/RuneLingual-Plugin, 원본: IaKee)에 한국어를 추가했다.
  - 플러그인 PR: YS-jack/RuneLingual-Plugin#25 (한국어), #26 (글자 이미지 언어 레이아웃 수정)
  - 번역 데이터 PR: YS-jack/Runelingual-Transcripts#29
  - 영어 원문 125,578줄 전부를 LLM(Opus)으로 번역하고, 대화 214배치를 NPC 단위로 Fable이 검수해 4,246줄을 고쳤다.
- RuneLingual 관리자(YS-jack)는 2026-05 이후 아무것도 병합하지 않았고, 마지막 응답은 2026-07-05이다. 위 PR들도 2026-11 현재 반응이 없다.
- 그래서 RuneLingual을 포크하지 않고, 구조를 개선한 **새 프로젝트**로 만든다. RuneLingual 코드는 참고만 한다(BSD 2-Clause이므로 가져다 쓰는 부분은 저작권 표기를 유지).

## 2. 확정된 결정

| 항목 | 결정 |
|---|---|
| 이름 | RuneHangul (`com.runehangul`, 설정 그룹 `runehangul`) |
| 언어 범위 | 한국어로 먼저 완성. 데이터 형식과 번역 파이프라인은 언어를 바꿔 끼울 수 있게 설계 |
| 실행 중 번역 | 1차에서 제외. DeepL 같은 외부 번역 API를 쓰지 않는다. 미리 LLM으로 번역해 둔 데이터만 쓴다 |
| 이름 표기 | 게임 속 이름은 `원문(한국어)`. 지어낸 이름은 음차, 실제 단어·아이템·스킬은 뜻, 주문은 음차. Black 등급 = 흑철, amulet = 아뮬렛, necklace = 목걸이 |
| 짧은 UI | 우클릭 행동, 버튼, 목록 화면(퀘스트·음악·이모트)은 한국어만 |
| 말투 | NPC별 캐릭터에 맞게. 플레이어 대사는 해요체 |

## 3. 설계 방향 (초안)

### 3.1 네트워크를 전혀 쓰지 않는다

번역 데이터와 폰트를 플러그인 jar 안에 넣는다. 2026-11-13 실측:

| 구성 | 크기 |
|---|---|
| 번역 데이터 125,578줄, gzip | 4.9MB (원본 16.4MB, xz 3.1MB) |
| Galmuri11 서브셋, 실제 쓰는 2,565자 | 0.5MB |
| Galmuri11 서브셋, 한글 11,172자 + ASCII + 자모 | 2.5MB |

합계 약 5.4~7.4MB로 Plugin Hub 플러그인 크기 제한(10MiB) 안에 든다. 효과:

- 외부 서버 통신이 없으니 `AGENTS.md`의 3rd-party 경고/옵트인 규칙이 적용되지 않는다.
- RuneLingual의 다운로드·해시·H2 DB 재생성 로직(과 macOS에서 열린 DB 파일을 지우는 버그)이 통째로 필요 없다.
- 데이터 업데이트는 플러그인 버전 업데이트(Plugin Hub PR)로 배포한다.

### 3.2 글자는 실행 중에 폰트로 그린다

OSRS 게임 폰트는 cp1252 범위만 있어 한글을 못 그린다. RuneLingual과 같이 글자를 이미지로 만들어 `ChatIconManager`에 등록하고 `<img=N>` 태그로 끼워 넣는 방식을 쓰되:

- 2만 장의 PNG를 내려받는 대신, jar에 넣은 서브셋 TTF로 필요한 글자(글자 × 색)를 처음 쓸 때 Java2D로 그려서 등록한다(지연 생성·캐시).
- 그릴 때 RuneLingual updater의 `update_char_images.py`와 같은 결과가 나오게 한다: 12px 캔버스, 안티앨리어싱 끔, 8가지 색(black/yellow/red/orange/white/lightblue/green/blue), 노란색만 그림자. 글자 폭·높이는 폰트 메트릭에서 잰다(쉼표·g·y처럼 아래로 내려가는 글자는 높이를 늘린다).
- 각 이미지의 실제 폭을 기록해 줄바꿈 계산에 쓴다(3.4).
- 한글 11,172자를 다 넣으면 DeepL이 아니라도 희귀 글자(똠, 햏, 뷁 …)가 `?`로 나오는 일이 없다. 크기와의 저울질은 열린 질문(6장).

### 3.3 번역 조회: 메모리 해시맵

- jar 안의 gzip 데이터를 시작할 때 백그라운드에서 읽어 메모리에 올린다(클라이언트 스레드에서 디스크 IO 금지).
- 키: 정규화한 영어 문장 + category (+ 필요 시 sub_category/source). RuneLingual의 P1~P5 단계 조회(정확 → 범주 완화 → 대소문자 무시 → 기호 제거 퍼지)를 참고하되, **데이터를 만들 때 정규화해 두고** 실행 중에는 해시 조회만 하도록 단순화한다.
- 숫자는 `<Num0>`, 색 태그는 `<colNum0>`, 플레이어 이름은 `[player name]` 자리로 바꿔서 조회하고 되돌린다(RuneLingual `Transformer`, `SqlQuery`, `DialogTranslator.replacePlayerNameWithPlaceholder` 참고).
- **성별 치환자 펼치기:** 원문 DB의 `[man/woman]`, `[sir/madam]` 같은 줄은 RuneLingual에서 영원히 매칭되지 않는다. 데이터를 만들 때 각 선택지로 펼친 영어 키를 모두 만든다.
- 메모리 사용량은 측정 필요(문자열 약 16MB → JVM에서 수십 MB 예상). 열린 질문(6장).

### 3.4 화면 반영 (RuneLingual에서 실제로 겪은 문제와 해결)

- **번역이 없으면 위젯을 건드리지 않는다.** 원문을 한글 폭으로 다시 줄바꿈하면 대화창이 깨진다(RuneLingual#26의 1번).
- **줄바꿈은 실제 픽셀 폭으로.** 글자 수 × 12px로 세면 영문·숫자가 섞인 문장이 일찍 끊긴다(#26의 2번).
- **설명 박스(KeepBr 위젯: 주문·기도 툴팁 등)**는 번역문의 `<br>`를 유지하되, 넘치는 줄만 다시 나누고 박스 높이를 줄 수에 맞춘다. "설명<br>멤버 전용"처럼 게임이 넣은 `<br>`가 있어도 마찬가지(#26의 3번).
- 한 줄짜리 목록 칸(퀘스트 목록)은 줄바꿈하지 말고, 번역을 짧게 한다(목록 이름은 한국어만).
- 기도 필터처럼 높이가 고정된 칸은 번역문을 줄여서 맞춘다.
- 마우스 툴팁에 원문과 번역을 함께 보여 주는 RuneLingual의 방식은 사용자가 좋아했다(번역 확인용).

### 3.5 빠진 문장 채우기

- 영어 원문 DB(transcript.db, 2025-05)에는 퀘스트 일지, 튜토리얼 안내 박스 문장이 없다.
- 플러그인은 번역을 못 찾은 문장을 로컬 파일로 모으고, 사용자가 **내보내기**로 제보할 수 있게 한다(자동 전송은 하지 않는다 — 외부 서버 금지 원칙과 개인정보).
- 제보된 문장은 오프라인 파이프라인(4장)으로 번역해 다음 버전에 넣는다.
- 장기적으로는 게임 캐시에서 아이템·NPC·오브젝트 이름과 인터페이스 문자열을 직접 뽑는 것도 검토(위키 기반보다 완전함). 대화는 서버가 보내므로 캐시에 없다.

### 3.6 하지 않는 것 (Plugin Hub 규칙)

- 채팅 입력창 자동 입력(한글 입력기, 로마자→한글 변환)은 "autotyping" 금지에 걸린다. RuneLingual의 일본어 입력 기능 같은 건 만들지 않는다.
- 나가는 채팅 메시지 수정 금지.
- JNA, 리플렉션, 외부 프로세스, 동적 코드 로딩 금지. 파일 IO는 `Filepath` 유틸리티로.
- 게임 입력 자동화 금지. 화면 캡처는 확인용으로만 쓰고, 기능이 동작하는지는 사용자가 게임에서 확인한다.

## 4. 번역 데이터와 파이프라인 (재사용할 자산)

이전 작업 폴더: `~/my_dev/runelingual-ko/`

| 경로 | 내용 |
|---|---|
| `translation/STYLE_GUIDE.md` | 번역 규칙(치환 기호, 이름 표기, 항목별 형식, 말투, 말장난 처리) |
| `translation/glossary.tsv` | 용어집 약 660개 |
| `translation/TRANSLATE_PROMPT.md`, `REVIEW_PROMPT.md` | 번역·검수 에이전트 지시서 |
| `translation/batches/` | 영어 원문을 298배치로 나눈 것 (`prepare_batches.py`) |
| `translation/done/`, `translation/reviews/` | 번역 결과와 검수 수정본. `reviews/zz_manual_fixes.tsv`는 게임 테스트로 고친 문구 |
| `translation/*.py` | `check_batch.py`, `check_review.py`, `apply_translations.py`, `batch_glossary.py`, `fix_particles.py` |
| `Runelingual-Transcripts/draft/ko/transcript_ko_*.tsv` | 최종 번역 데이터 (english, translation, category, sub_category, source) |
| `Runelingual-Transcripts/updater/transcript.db` | 영어 원문 DB (SQLite, 125,578줄) |
| `Runelingual-Transcripts/updater/fonts/ko/Galmuri11.ttf` | 폰트 (SIL OFL 1.1, `OFL.txt`) |
| `RuneLingual-Plugin/` | RuneLingual 소스 (`add-korean` 브랜치에 우리 수정 포함) |

번역 데이터의 출처와 라이선스:
- 영어 원문 텍스트는 게임(Jagex) 텍스트다. transcript.db와 updater 스크립트는 Runelingual-Transcripts(MIT)에서 왔다. 새 프로젝트에 데이터를 넣을 때 출처를 README에 밝힌다.
- 한국어 번역은 우리가 만든 것이다.

## 5. 개발 환경 (macOS에서 확인한 것)

- JDK: 이 Mac에 Temurin 21. 플러그인은 Java 11 타깃(`options.release.set(11)`).
- 실행: `./gradlew run` (템플릿에 있음) 또는 `./gradlew shadowJar` 후
  `java -ea --add-exports java.desktop/com.apple.eawt=ALL-UNNAMED --add-opens java.desktop/com.apple.eawt=ALL-UNNAMED -cp build/libs/runehangul-unspecified-all.jar com.runehangul.RuneHangulPluginTest --developer-mode`
  (macOS + JDK 21에서는 `com.apple.eawt` 옵션이 없으면 시작 단계에서 실패한다.)
- Jagex 계정 로그인: RuneLite 설정 창(`/Applications/RuneLite.app/Contents/MacOS/RuneLite --configure`)에서 Client arguments에 `--insecure-write-credentials`를 넣고 Jagex Launcher로 한 번 실행하면 `~/.runelite/credentials.properties`가 생긴다. 이 파일은 로그인 토큰이므로 읽거나 출력하지 않는다. 참고: https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts
- RuneLite 설정 프로필은 클라우드 동기화된다. 프로필 파일을 직접 고치지 말고 클라이언트 화면에서 설정한다.
- 개발 클라이언트는 시스템 JDK로 돌아서, RuneLite 설정 창의 드롭다운 등에서 한글이 깨져 보일 수 있다(정식 RuneLite 번들 JRE에서는 괜찮음).
- Windows: 같은 shadow jar를 `%LOCALAPPDATA%\RuneLite\jre\bin\java.exe`로 실행할 수 있다. 다만 한 Windows PC에서 "Initializing Discord RPC service."에서 멈추는 문제가 있었고 원인은 아직 모른다(스레드 덤프 필요).
- 정식 RuneLite(Jagex Launcher)로는 개발 중인 플러그인을 띄울 수 없다: sideloading은 개발자 모드에서만 되고, 개발자 모드는 정식 런처로 실행하면 막힌다(RuneLite.java의 `developerMode` 조건).

## 6. 열린 질문

- 한글 글자 범위: 실제 쓰는 글자만(0.5MB) vs 한글 전체(2.5MB). 나중에 채팅 번역 같은 기능을 붙이면 전체가 필요하다.
- 메모리 사용량: 12만 줄 해시맵이 RuneLite에서 허용할 만한 수준인지 측정.
- 데이터 업데이트 주기: 플러그인 업데이트마다 5MB를 다시 받는 셈. Plugin Hub 쪽에 문제가 없는지.
- 영어 원문의 최신화: transcript.db(2025-05)보다 큰 원본이 있는지(중국어 데이터는 인터페이스 22k줄로 더 큼). 게임 캐시 추출로 대체할지.
- 번역 데이터 저장소를 플러그인 저장소와 분리할지, 한 저장소 안의 `data/`로 둘지.
- RuneLingual 관리자가 돌아오면 #25/#26/#29를 어떻게 할지.

## 7. 작업 순서 (제안)

1. **뼈대 실행 확인:** 빈 플러그인을 개발 클라이언트에서 띄운다.
2. **글자 렌더링:** 서브셋 폰트를 jar에 넣고, 글자 이미지를 실행 중에 그려 채팅 아이콘으로 등록한다. 채팅창에 한글 한 줄을 띄워 확인한다.
3. **데이터:** 기존 번역(tsv)을 정규화·성별 펼치기를 거쳐 gzip 파일로 만드는 빌드 스크립트. 메모리 해시맵 로더와 조회.
4. **NPC 대화 번역:** 대화창(NPC·플레이어·선택지)부터. 번역이 없으면 손대지 않는다.
5. **메뉴·이름·조사 문구·인터페이스:** 하나씩 넓힌다. 3.4의 레이아웃 규칙을 처음부터 적용한다.
6. **빠진 문장 수집·내보내기.**
7. **Plugin Hub 제출 준비:** 이름, 설명, 아이콘, README(출처·라이선스), 크기 확인.
