#!/usr/bin/env bash
# 커밋 전에 한 번: 사람이든 AI든 "다 됐다"고 말하기 전에 이게 통과해야 한다.
#   ./verify.sh          전부 (백엔드 테스트 + 프론트 lint·테스트·빌드)
#   ./verify.sh front    프론트만 (몇 초)
#   ./verify.sh back     백엔드만
#   ./verify.sh rules    구조 규칙만 (ArchUnit·스키마 검사, 빠름)
set -uo pipefail
cd "$(dirname "$0")"

target="${1:-all}"
failed=()

step() {
  local name="$1"; shift
  printf '\n\033[1m▶ %s\033[0m\n' "$name"
  if "$@"; then printf '\033[32m✓ %s\033[0m\n' "$name"; else printf '\033[31m✗ %s\033[0m\n' "$name"; failed+=("$name"); fi
}

front() {
  [ -d frontend/node_modules ] || (cd frontend && npm ci)
  step "프론트 규칙 (ESLint)" bash -c 'cd frontend && npm run -s lint'
  step "프론트 테스트" bash -c 'cd frontend && npm test --silent'
  step "프론트 빌드 (타입 검사 포함)" bash -c 'cd frontend && npm run -s build >/dev/null'
}

back() {
  step "백엔드 테스트 (구조 규칙 포함)" bash -c 'cd backend && ./gradlew test --console=plain -q'
}

rules() {
  step "구조 규칙 (ArchUnit·스키마)" bash -c "cd backend && ./gradlew test --console=plain -q --tests 'com.bandmong.harness.*'"
  step "프론트 규칙 (ESLint)" bash -c 'cd frontend && npm run -s lint'
}

case "$target" in
  front) front ;;
  back) back ;;
  rules) rules ;;
  all) front; back ;;
  *) echo "사용법: ./verify.sh [all|front|back|rules]"; exit 2 ;;
esac

echo
if [ ${#failed[@]} -eq 0 ]; then
  printf '\033[32m모두 통과\033[0m\n'
else
  printf '\033[31m실패: %s\033[0m\n' "${failed[*]}"
  echo "실패 메시지의 '고치는 법'을 따라 고친 뒤 다시 실행하세요. 백엔드 상세: backend/build/reports/tests/test/index.html"
  exit 1
fi
