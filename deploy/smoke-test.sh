#!/usr/bin/env bash
# docker compose로 띄운 전체 묶음(nginx → Spring → MySQL)에 실제 요청을 보내 본다.
#   BASE=http://localhost:8000 deploy/smoke-test.sh
set -euo pipefail
BASE=${BASE:-http://localhost:8000}
JAR=$(mktemp)

echo "1) 서버가 UP 될 때까지 기다리기"
for i in $(seq 1 60); do
  if curl -fsS "$BASE/actuator/health" 2>/dev/null | grep -q UP; then break; fi
  [ "$i" = 60 ] && { echo "서버가 뜨지 않음"; exit 1; }
  sleep 3
done

echo "2) 화면(index.html)과 React 라우터 주소"
curl -fsS "$BASE/" | grep -q '<div id="root">'
curl -fsS "$BASE/bands/123" | grep -q '<div id="root">'

echo "3) CSRF 쿠키 받기 → 개발용 로그인 → 내 정보"
curl -fsS -c "$JAR" "$BASE/api/auth/providers" | grep -q '"devLogin":true'
XSRF=$(awk '$6=="XSRF-TOKEN"{print $7}' "$JAR")
curl -fsS -b "$JAR" -c "$JAR" -H "X-XSRF-TOKEN: $XSRF" -H 'Content-Type: application/json' \
  -d '{"name":"smoke"}' "$BASE/api/dev/login" > /dev/null
curl -fsS -b "$JAR" "$BASE/api/me" | grep -q '"provider":"dev"'

echo "4) 프로필 저장 (한글이 MySQL을 거쳐 그대로 돌아오는지)"
curl -fsS -b "$JAR" -H "X-XSRF-TOKEN: $XSRF" -H 'Content-Type: application/json' -X PUT \
  -d '{"nickname":"스모크","instruments":["DRUM"],"level":"HOBBY"}' "$BASE/api/me/profile" | grep -q '"nickname":"스모크"'

echo "5) 합주실 예시 데이터 (Flyway + 시드)"
curl -fsS -b "$JAR" "$BASE/api/studios" | grep -q '합정 그루브 합주실'

echo "6) 실시간 연결이 nginx를 지나 바로 오는지 (버퍼링 없이)"
OUT=$(curl -sN -b "$JAR" --max-time 3 "$BASE/api/stream" || true)
echo "$OUT" | grep -q 'event:ready'

echo "모두 통과"
