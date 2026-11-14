# 내장 번역 데이터

`data/<언어>/*.tsv`는 플러그인 jar에 들어가는 내장 번역의 원본이다. `./gradlew bundleTranslations`가 이 파일들을 `src/main/resources/com/runetongue/translations_<언어>.tsv.gz` 하나로 묶는다. **데이터를 고친 뒤에는 이 작업을 돌리고 gz도 함께 커밋한다.** Plugin Hub는 `build=standard` 플러그인을 자기 `build.gradle`로 빌드하므로 빌드 중 생성 작업이 돌지 않는다. 잊으면 `BundledTranslationsTest`가 실패한다. 같은 데이터면 같은 바이트가 나온다.

- 형식은 사용자 번역 폴더와 같다(`docs/CONTEXT.md` 3.7): 머리글 `english, translation, category, sub_category, source`, UTF-8.
- 한 언어 폴더의 파일은 머리글이 모두 같아야 한다. 파일 이름순으로 이어 붙이므로 뒤 파일이 이긴다.
- 플러그인이 실행 중에 건너뛸 줄(치환 기호 불일치 등)은 번들에서 빼고 `build/reports/translations/<언어>-skipped.tsv`에 적는다. 이 목록을 보고 원본 줄을 고친다.
- 정규화하면 키가 같아지는 줄(`Take-1`, `Take-5` → `Take-<Num0>`)은 뒤 줄 하나만 남는다. 번역도 숫자 자리만 다르면 보고하지 않는다.

## 출처와 라이선스

| 데이터 | 출처 | 라이선스 |
|---|---|---|
| 영어 원문 | 게임 텍스트(Jagex). [Runelingual-Transcripts](https://github.com/YS-jack/Runelingual-Transcripts)의 `transcript.db`(2025-05)에서 가져왔다 | 저장소 README는 MIT라고 적지만 LICENSE 파일은 없다 |
| 그중 이름·조사 문구·행동 | Chisel(Weird Gloop) MOID 데이터 | |
| 그중 NPC 대화(`dialogue`) | [OSRS 위키](https://oldschool.runescape.wiki/) 대화 기록 페이지 | CC BY-NC-SA 3.0 |
| 한국어 번역 (`ko`) | 이 프로젝트에서 만들었다(LLM 번역 + 검수, `docs/CONTEXT.md` 1장·4장) | |

위키에서 온 대화를 번들해 배포할 때 지켜야 할 조건(출처 표기, 비상업, 같은 조건 배포)은 `docs/CONTEXT.md` 6장의 열린 질문에 있다.
