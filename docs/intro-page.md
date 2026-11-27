# 소개 페이지 (`/intro/`)

밴드몽을 처음 보는 사람용 랜딩 페이지. 앱 화면은 그대로 두고, [taste-skill](https://github.com/Leonxlnx/taste-skill) 2.5.1의 규칙과 검사로 따로 만들었다.

- 위치: `frontend/public/intro/` (빌드하면 `dist/intro/`, 운영에서 `https://<도메인>/intro/`)
- 로그인 화면(웹) 아래 "밴드몽이 처음이라면 소개 보기"에서 들어온다. "무료로 시작하기"는 앱(`/`)으로 간다.
- 그림은 모두 실제 밴드몽 화면 캡처(가짜 API 데이터로 찍음): 내 밴드, 채팅 신고·차단, 공연 큐시트, 내 정보

## 딜 결과 (taste-skill은 고르지 않고 무작위로 뽑는다)

| 항목 | 결과 |
|---|---|
| 종류 | software |
| 히어로 | split (글 왼쪽, 앱 화면 오른쪽) |
| 색 | 앱 고유색: 잉크 #16181D, 앰버 #B87400 (앱의 #F5B323을 바탕 대비 3:1이 되게 어둡게) |
| 글꼴 | IBM Plex Sans KR + IBM Plex Mono (뽑힌 Familjen Grotesk에 한글이 없어 같은 계열로 교체) |
| 효과 | 히어로 화면 뒤 silk-folds, 기능 카드 뒤 fluted-glass (멈춘 그림) |
| 모먼트 | liquid-type: "단톡방 대신 합주 일정표" 위에 마우스를 올리면 물결 |
| 내비 / 마무리 / 푸터 / 버튼 | nav-centre / close-type / footer-dark / arrow |

## 검사 결과 (2026-11-27)

`GATE: PASS`. 예외 2건(waiver): 히어로 스트레스 테스트가 h1을 라틴 글자 45자 폭으로 채우는데, 한글은 글자 폭이 두 배라 맞지 않음.
남은 경고 중 한국어 때문에 생기는 것: h1 단어 수 0(한글을 단어로 세지 못함), 내비 현재 위치 표시 없음.

## 고칠 때

1. 글을 바꾸면 글꼴을 다시 자른다 (이 페이지에 쓰인 글자만 담은 woff2라서 새 글자가 빠진다):
   ```bash
   npm pack @fontsource/ibm-plex-sans-kr@5.1.0 @fontsource/ibm-plex-mono@5.1.0   # 풀어서
   python3 frontend/tools/intro-fonts.py <ibm-plex-sans-kr/package> <ibm-plex-mono/package>
   ```
2. `assets/`는 taste-skill 원본 그대로다. 고치지 말고, 페이지 전용 스타일은 `index.html`의 `<style>`에 둔다.
3. taste-skill로 다시 검사: `node <skill>/scripts/gate.mjs index.html --root <dist 폴더>`
