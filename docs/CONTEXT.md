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
| 언어 범위 | 한국어로 먼저 완성. 데이터 형식과 번역 파이프라인은 언어를 바꿔 끼울 수 있게 설계. 1차에서 사용자 번역 폴더는 한국어와 게임 폰트(cp1252)로 그려지는 언어(스페인어·포르투갈어·프랑스어·독일어 등)까지 받는다. 러시아어·일본어처럼 다른 폰트가 필요한 문자는 1차 범위 밖 (2026-11-13) |
| 사용자 번역 폴더 | **번역 파일을 PC의 정해진 폴더에 넣기만 하면 플러그인이 읽는다.** 커스텀 빌드, 호스팅, 해시 파일, 관리자 승인이 필요 없다. 사용자 파일은 내장 번역을 덮어쓴다. RuneLingual에서 손번역자가 겪은 진입 장벽(커스텀 빌드, GitHub 호스팅, 엑셀→tsv→해시 스크립트, 관리자 없으면 배포 불가)을 없애려는 것 (2026-11-13, 3.7) |
| 실행 중 번역 | 1차에서 제외. DeepL 같은 외부 번역 API를 쓰지 않는다. 미리 LLM으로 번역해 둔 데이터만 쓴다 |
| 이름 표기 | 게임 속 이름은 `원문(한국어)`. 지어낸 이름은 음차, 실제 단어·아이템·스킬은 뜻, 주문은 음차. Black 등급 = 흑철, amulet = 아뮬렛, necklace = 목걸이 |
| 짧은 UI | 우클릭 행동, 버튼, 목록 화면(퀘스트·음악·이모트)은 한국어만 |
| 말투 | NPC별 캐릭터에 맞게. 플레이어 대사는 해요체 |
| 구현 언어 | 플러그인은 Java 11(RuneLite·Plugin Hub 요구, JNI/JNA 금지라 다른 언어 코드를 실행할 수 없음). 번역 번들 생성기도 Java로 만들어 조회 키 정규화 코드를 플러그인과 함께 쓴다(Gradle의 별도 작업, 플러그인 jar에는 넣지 않음). 번역 작업용 보조 스크립트는 당분간 기존 Python(`~/my_dev/runelingual-ko/translation/`)을 쓰고 필요할 때 옮긴다. Rust는 검토했으나 얻는 게 적어 쓰지 않기로 함 (2026-11-13) |

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
- 그릴 때 RuneLingual updater의 `update_char_images.py`와 같은 결과가 나오게 한다: 12px 캔버스, 안티앨리어싱 끔, 8가지 색(black/yellow/red/orange/white/lightblue/green/blue), 노란색만 그림자. 글자 폭·높이는 폰트 메트릭에서 잰다(쉼표·g·y처럼 아래로 내려가는 글자는 높이를 늘린다). (2단계에서 구현: RuneLingual PNG 20,520장과 픽셀 단위로 같음을 확인)
- 그림자 색은 순수 검정이 아니라 `0x010101`. RuneLite의 `ImageUtil.getImageIndexedSprite`가 불투명한 `0x000000`을 투명 팔레트로 바꾸기 때문에, 순수 검정 그림자는 게임에서 보이지 않는다(RuneLingual도 같은 문제).
- `ChatIconManager.registerChatIcon`은 다음 틱에야 `<img=N>` 번호가 정해지므로, 클라이언트 스레드에서 `reserveChatIcon` + `updateChatIcon`으로 바로 번호를 받는다. 채팅 아이콘은 지울 수 없으니 캐시는 싱글턴으로 두어 플러그인을 껐다 켜도 다시 등록하지 않는다.
- 각 이미지의 실제 폭을 기록해 줄바꿈 계산에 쓴다(3.4).
- 한글 11,172자를 다 넣으면 DeepL이 아니라도 희귀 글자(똠, 햏, 뷁 …)가 `?`로 나오는 일이 없다. 크기와의 저울질은 열린 질문(6장).

### 3.3 번역 조회: 메모리 해시맵

- jar 안의 gzip 데이터를 시작할 때 백그라운드에서 읽어 메모리에 올린다(클라이언트 스레드에서 디스크 IO 금지).
- 내장 데이터는 사용자 번역 파일(3.7)과 **같은 tsv 형식**을 gzip으로 묶은 것이고, 같은 파서·정규화 코드로 읽는다. 처음에는 실행 중에 정규화한다. 12만 줄 정규화가 백그라운드에서 2초쯤을 넘으면 빌드할 때 미리 정규화해 두는 쪽으로 바꾼다.
  - 2026-11-13 측정(Apple M5 Pro): 따로 돌리면 1.1~1.4초(대부분 정규화), 개발 클라이언트 시작 중에는 1.9초. 백그라운드라 클라이언트 시작을 막지 않고, 로그인 화면에 있는 동안 끝난다. 당분간 실행 중 정규화를 유지하고, 느린 PC에서 문제가 보이면 미리 정규화로 바꾼다.
  - 원본은 `data/<언어>/*.tsv`, `./gradlew bundleTranslations`(빌드 때 자동)가 `translations_<언어>.tsv.gz`로 묶는다. 실행 중에 건너뛸 줄은 번들에서 빼고 `build/reports/translations/`에 목록을 쓴다(`data/README.md`).
- 키: 정규화한 영어 문장 + category (+ 필요 시 sub_category/source). 정규화한 영어 문장 하나에 범위(category, sub_category, source)가 다른 줄들을 모아 두고, 정확한 범위 → 느슨한 범위 순서로 고른다(순서는 3.7 우선순위). RuneLingual의 P1~P5 단계 조회(정확 → 범주 완화 → 대소문자 무시 → 기호 제거 퍼지)를 참고하되, 실행 중에는 해시 조회만 하도록 단순화한다.
- 숫자는 `<Num0>`, 색 태그는 `<colNum0>`, 플레이어 이름은 `[player name]` 자리로 바꿔서 조회하고 되돌린다(RuneLingual `Transformer`, `SqlQuery`, `DialogTranslator.replacePlayerNameWithPlaceholder` 참고).
- **성별 치환자 펼치기:** 원문 DB의 `[man/woman]`, `[sir/madam]` 같은 줄은 RuneLingual에서 영원히 매칭되지 않는다. 데이터를 만들 때 각 선택지로 펼친 영어 키를 모두 만든다.
- 메모리: 조회표 125,784개가 힙에 약 35MB 남는다(2026-11-13 측정). jar는 5.0MB(번역 gz 4.9MB + 폰트 0.5MB).

### 3.4 화면 반영 (RuneLingual에서 실제로 겪은 문제와 해결)

- **번역이 없으면 위젯을 건드리지 않는다.** 원문을 한글 폭으로 다시 줄바꿈하면 대화창이 깨진다(RuneLingual#26의 1번).
- **줄바꿈은 실제 픽셀 폭으로.** 글자 수 × 12px로 세면 영문·숫자가 섞인 문장이 일찍 끊긴다(#26의 2번).
- **설명 박스(KeepBr 위젯: 주문·기도 툴팁 등)**는 번역문의 `<br>`를 유지하되, 넘치는 줄만 다시 나누고 박스 높이를 줄 수에 맞춘다. "설명<br>멤버 전용"처럼 게임이 넣은 `<br>`가 있어도 마찬가지(#26의 3번).
- 한 줄짜리 목록 칸(퀘스트 목록)은 줄바꿈하지 말고, 번역을 짧게 한다(목록 이름은 한국어만).
- 기도 필터처럼 높이가 고정된 칸은 번역문을 줄여서 맞춘다.
- 마우스 툴팁에 원문과 번역을 함께 보여 주는 RuneLingual의 방식은 사용자가 좋아했다(번역 확인용).

### 3.5 빠진 문장 채우기

- 영어 원문 DB(transcript.db, 2025-05)에는 퀘스트 일지, 튜토리얼 안내 박스 문장이 없다.
- 플러그인은 번역을 못 찾은 문장을 로컬 파일로 모으고, 사용자가 **내보내기**로 제보할 수 있게 한다.
- **제보는 사용자가 내보낸 파일을 직접 보내는 방식으로 한다(2026-11-13 확정).** 사용자가 파일을 Discord나 GitHub에 올리면 우리가 받는다. 플러그인은 네트워크를 쓰지 않는다(3.1).
  - RuneLite가 네트워크를 막는 것은 아니다. Plugin Hub 규칙상 외부 서버로 보내는 기능은 기본값 꺼짐과 IP 전송 경고가 필요하고, 다른 플레이어 데이터 수집과 플레이어 정보의 HTTP 전송은 금지다. 우리는 서버 운영과 개인정보 책임을 지지 않으려고 쓰지 않는다.
  - 나중에 사용자가 늘면 `LinkBrowser`로 내용이 채워진 GitHub 이슈 화면을 여는 방식을 더할 수 있다(보내는 것은 사용자의 브라우저와 클릭, 주소 길이 제한으로 한 번에 수십 줄). 자동 업로드 서버는 수동 제출로 감당이 안 될 때 다시 검토한다.
  - 저장하기 전에 플레이어 이름은 `[player name]`으로 바꾸고, 채팅(공개·친구·클랜)은 모으지 않는다. 다른 사람의 이름이나 대화가 섞이지 않게 하려는 것이다.
- 내보내기 파일은 사용자 번역 파일(3.7)과 같은 형식으로 `missing/<언어>/`에 쓴다(머리글 있음, english는 정규화된 상태, translation은 빈 칸, category 채움). 그래서 "내보내기 → 번역 칸 채우기 → `translations/<언어>/`로 옮기기 → 다시 불러오기"가 그대로 이어진다.
- 제보된 문장은 오프라인 파이프라인(4장)으로 번역해 다음 버전에 넣는다.
- 장기적으로는 게임 캐시에서 아이템·NPC·오브젝트 이름과 인터페이스 문자열을 직접 뽑는 것도 검토(위키 기반보다 완전함). 대화는 서버가 보내므로 캐시에 없다.

### 3.7 사용자 번역 폴더 (2026-11-13 확정)

배경: Discord에서 스페인어 번역자가 "I came here hoping it'd be way more simple to hand translate and plug and drop a tsv file."라고 했다. RuneLingual은 직접 번역한 파일을 쓰려면 커스텀 빌드, GitHub 호스팅, 엑셀→tsv→해시 스크립트가 필요했고, 관리자가 없으면 배포도 못 했다.

**폴더 위치**

```
~/.runelite/plugin-data/runehangul/      (PluginDescriptor internalName = "runehangul", Plugin.getPluginDirectory())
├── translations/<언어>/   번역 파일을 넣는 곳 (ko, es, pt, fr, de …)
└── missing/<언어>/        빠진 문장 내보내기 (3.5)
```

- 파일 IO는 모두 `Filepath`로. 설정에서 언어를 고른다(기본 한국어). 한국어는 내장 번역 + 폴더 파일, 내장 번역이 없는 언어는 폴더 파일만 쓴다.
- 사이드 패널: **폴더 열기**(`LinkBrowser.open`), **파일 추가**(`Filepath.Chooser`로 고른 tsv를 폴더에 복사 후 다시 불러오기), **다시 불러오기**, 불러오기 결과. 패널 아이콘은 GlyphRenderer로 "한"을 그려 쓴다(PNG 없음).
- RuneLite의 Swing 기본 폰트(RuneScape 폰트)에는 한글이 없다. 패널 글자는 `FontManager.getDefaultFont()`(OS 폰트로 대체해 그림)로 그린다. 설정 창은 우리가 폰트를 못 바꾸므로 설정 이름·선택지에 한글을 쓰지 않는다(언어 이름은 `Korean`).
- `Filepath.Chooser`가 돌려준 경로는 고른 파일 범위로 묶여 있어 `getParent()`가 예외를 던진다. 복사할 때는 파일 이름만 쓴다.

**파일 형식**

- 탭 구분 텍스트(tsv). UTF-8(BOM 있어도 됨), 엑셀 "유니코드 텍스트"(UTF-16, BOM 있음)도 읽는다.
- 필수 열은 `english`, `translation` 두 개. `category`, `sub_category`, `source`는 선택.
- 첫 줄 첫 칸이 `english`면 머리글로 보고 그 순서대로 읽는다. 없으면 `english, translation, category, sub_category, source` 순서.
- 내장 번들과 같은 정규화(숫자 `<Num0>`, 색 태그 `<colNum0>`, 플레이어 이름 `[player name]`, `[man/woman]` 펼치기)를 적용해 내장 데이터와 똑같이 매칭된다. 번역에 `[A/B]`가 같은 개수로 있으면 순서대로 짝짓고, 없으면 모든 펼친 키에 같은 번역을 쓴다.

**우선순위**

1. 조회는 범위(category, sub_category, source)와 함께 묻는다. 아래 순서로 처음 맞는 줄을 쓴다.
   1. 사용자 파일에서 **정확히 맞는** 줄: 묻는 범위 그대로이거나 그보다 넓은 범위의 줄, 좁은 쪽부터. category를 비운 줄은 모든 범위에 정확히 맞는다.
   2. 내장 번역에서 정확히 맞는 줄.
   3. 사용자 파일에서 **느슨하게 맞는** 줄: 앞쪽 칸이 같은 만큼 고른다. 예를 들어 (dialogue, Hans, Hans)로 물으면 (dialogue, Hans, Player) → (dialogue, 다른 NPC) → 다른 category 순서.
   4. 내장 번역에서 느슨하게 맞는 줄.
2. 그래서 category를 비운 사용자 줄은 내장 번역을 어디서나 이긴다. category를 적은 사용자 줄은 그 category 안에서만 내장 번역을 이기고, 다른 category의 정확한 내장 번역은 덮지 않는다.
3. 파일이 여러 개면 파일 이름순으로 읽고 나중 파일이 이긴다. 한 파일 안의 중복 키는 뒤의 줄이 이기고, 앞 줄은 "덮어씀"으로 보고한다.

**다시 불러오기**

- 플러그인을 켤 때, 패널 버튼, `::rh reload`. 읽기와 맵 만들기는 백그라운드 스레드에서 하고, 다 만들면 불변 맵 참조 하나를 바꿔 끼운다. 클라이언트 스레드는 조회만 한다.
- `shutDown()`에서 진행 중인 불러오기를 취소하고, 패널 버튼을 없애고, 맵을 비운다.

**오류 처리**

- 줄 단위로 검사해 잘못된 줄만 건너뛴다: 열 부족, english 비어 있음, 치환 기호 집합이 원문과 다름(순서는 상관없음), `[A/B]` 선택지 개수 불일치, 인코딩 오류.
- 번역 칸이 빈 줄은 오류가 아니라 "미번역"으로 센다(반쯤 채운 내보내기 파일을 그대로 넣을 수 있게).
- 불러온 뒤 채팅창에 한 줄 요약(파일 수, 불러온 줄, 건너뛴 줄), 패널에 파일별로 건너뛴 줄 번호와 이유(파일당 최대 50줄).

**구현 구조**

- 파서·정규화·치환 기호 검사는 RuneLite에 의존하지 않는 순수 코드(`com.runehangul.text`)로 두고, 플러그인과 번들 생성기가 함께 쓴다. JUnit으로 검증한다.
- 화면에 그릴 때 cp1252로 표현되는 글자는 게임 폰트, 한글은 Galmuri 이미지, 둘 다 아닌 글자는 `?`(2단계 `GlyphIcons` 동작 그대로).

### 3.8 하지 않는 것 (Plugin Hub 규칙)

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
- 메모리 사용량: 측정해 보니 약 35MB(3.3). RuneLite 기본 힙에서 문제가 되는지는 오래 켜 두고 지켜본다.
- 데이터 업데이트 주기: 플러그인 업데이트마다 5MB를 다시 받는 셈. Plugin Hub 쪽에 문제가 없는지.
- 영어 원문의 최신화: transcript.db(2025-05)보다 큰 원본이 있는지(중국어 데이터는 인터페이스 22k줄로 더 큼). 게임 캐시 추출로 대체할지.
- 번역 데이터 저장소: 당분간 한 저장소 안의 `data/`에 둔다(2026-11-13). 번들 생성기가 바로 읽을 수 있다. 커지거나 번역자가 늘면 분리를 다시 본다.
- 위키 데이터 라이선스: 내장 대화의 영어 원문은 OSRS 위키(CC BY-NC-SA 3.0)에서 왔다. 번들해 배포하려면 출처 표기, 비상업, 같은 조건 배포를 지켜야 하고, 플러그인 코드(BSD-2)와 데이터 라이선스를 따로 적어야 할 수 있다. Plugin Hub가 이를 문제 삼는지 확인 필요. RuneLingual-Transcripts는 README에 MIT라고 적었지만 LICENSE 파일이 없다(`data/README.md`).
- RuneLingual 관리자가 돌아오면 #25/#26/#29를 어떻게 할지.

## 7. 작업 순서 (제안)

1. **뼈대 실행 확인:** 빈 플러그인을 개발 클라이언트에서 띄운다. (완료 2026-11-13)
2. **글자 렌더링:** 서브셋 폰트를 jar에 넣고, 글자 이미지를 실행 중에 그려 채팅 아이콘으로 등록한다. 채팅창에 한글 한 줄을 띄워 확인한다. (완료 2026-11-13, `::hangul` 명령으로 8색 확인)
3. **데이터와 로더 (사용자 번역 폴더 포함, 3.7):** (완료 2026-11-13)
   1. 정규화·tsv 파서·치환 기호 검사(순수 코드 + JUnit).
   2. 번역 층(사용자 파일 > 내장)과 우선순위 규칙을 담은 조회 맵.
   3. 사용자 폴더 불러오기: `internalName`, `Filepath`, 백그라운드 읽기, `::rh reload`, 채팅 요약. (완료 2026-11-13)
   4. 사이드 패널: 폴더 열기, 파일 추가, 다시 불러오기, 결과 보기. 설정의 언어 선택. (완료 2026-11-13)
   5. 내장 번들: 기존 번역 tsv를 gzip으로 묶는 Gradle 작업(플러그인 jar에는 생성기 코드를 넣지 않음). 시작 시간·메모리 측정. (완료 2026-11-13)
4. **NPC 대화 번역:** 대화창(NPC·플레이어·선택지)부터. 번역이 없으면 손대지 않는다.
   1. 대화창 번역: NPC·플레이어 대사, 선택지, NPC 이름, 계속/선택 안내. (dialogue, NPC 이름, 화자) 범위로 찾고, 실제 픽셀 폭으로 줄을 나누고, 위젯 글자색이 바뀌면 글자 이미지를 다시 그린다. (완료 2026-11-13)
   2. 대화창 빠진 문장 수집: 6단계의 수집을 대화창에 먼저 붙였다. `missing/<언어>/missing.tsv`에 한 번씩 쓴다. 첫 수집 예: 퀘스트 뒤 Sigmund 대사(위키 대화 기록에 없는 분기). (완료 2026-11-13)
5. **메뉴·이름·조사 문구·인터페이스:** 하나씩 넓힌다. 3.4의 레이아웃 규칙을 처음부터 적용한다.
   1. 우클릭 메뉴: 행동과 대상 이름. 다른 플러그인이 영어 글자로 메뉴를 바꾸고 고르므로, 그리기 직전(`BeforeRender`)과 메뉴를 열 때(`MenuOpened`, 늦은 우선순위)에만 바꾸고, 클릭하면 다른 플러그인보다 먼저(`MenuOptionClicked`, 이른 우선순위) 영어로 되돌린다. 플레이어 이름과 RuneLite 플러그인이 넣은 메뉴(`RUNELITE*`)는 건드리지 않는다. 메뉴를 열기 전에 바꿔 두므로 상자 폭도 한국어 기준으로 잡힌다. (완료 2026-11-13)
   2. 채팅창 게임 메시지: 조사 문구, 게임 안내, 레벨업, NPC 말. 플레이어가 친 채팅·방송·알림은 건드리지 않는다. 글자색은 채팅창 투명 설정에 따라 검정/흰색. (완료 2026-11-14)
   3. 대화창 계열 상자: 안내 상자(제목 있음/없음), 아이템·그림 상자, 레벨업 알림. (완료 2026-11-14)
   4. 인터페이스 전반: RuneLingual의 `Ids`·`WidgetCapture` 규칙을 `InterfaceRules`로 옮겼다(탭별 범위, 이름 위젯, `<br>` 유지·줄별 번역·고정 폭, 번역하지 않는 위젯과 다른 플레이어 이름 판별, 채팅 탭 버튼 정렬). 툴팁 상자 크기 조정은 `Widget2ModDict`를 그대로 가져와 글자 폭만 실제 픽셀로 바꿨다. 인터페이스 키는 색 태그(`<colNum0>`)와 위젯에 따라 `<br>`를 남긴 채 찾는다. 번역한 위젯은 줄 높이 12. 처음에 이 규칙들을 옮기지 않고 새로 짜서 기도 툴팁·퀘스트 목록이 깨졌다: **RuneLingual에 이미 있는 화면별 처리는 먼저 그 코드를 읽고 옮긴다.** (완료 2026-11-14, 부분 번역 `PartialTranslationManager`는 남음)
6. **빠진 문장 수집·내보내기:** 3.7과 같은 형식으로 `missing/<언어>/`에 쓴다. 대화창은 4단계에서 붙였고, 5단계의 메뉴·채팅·인터페이스에도 같은 수집을 붙였다. 다시 불러올 때마다 이제 번역되는 줄은 파일에서 지우고, 패널에 남은 줄 수를 보여 준다. 다른 플레이어 이름이 섞일 수 있는 곳(채팅·친구·클랜 목록, 플레이어 메뉴, 틀에 맞는 문장, 13자 미만 인터페이스 글자)은 수집하지 않는다. (완료 2026-11-14)
7. **Plugin Hub 제출 준비:** 이름, 설명, 아이콘, README(출처·라이선스), 크기 확인.
   - 2026-11-14: README(영어, 손번역자용 폴더 사용법·형식·빠진 문장 수집, 출처·라이선스), `icon.png`(48×72, `./gradlew makeIcon`이 Galmuri "한"을 4배로 그림), 플러그인 설명 정리, 깨끗한 빌드 확인(jar 5.07MB, 생성기 미포함). 플러그인 자체 문구(패널·채팅 안내·건너뛴 줄 이유)는 한국어가 아닌 언어를 고르면 영어로 나온다.
   - 남은 것: 이름("RuneHangul"이 스페인어 사용자에게 낯설 수 있음), 위키 데이터 라이선스(6장), Plugin Hub PR.
