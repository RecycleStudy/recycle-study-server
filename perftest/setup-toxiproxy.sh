#!/bin/bash
#
# Toxiproxy SMTP 지연 설정 스크립트
#
# Gmail SMTP 실측 결과 (2026-02-10, 15건 발송):
#   Median: 3,163 ms / Stdev: 257 ms / Range: 2,641~3,832 ms
#
# 사용법:
#   docker-compose up -d 이후 실행
#   bash perftest/setup-toxiproxy.sh

TOXIPROXY_API="http://localhost:8474"
PROXY_NAME="smtp"
LATENCY=3163
JITTER=257

echo "Toxiproxy SMTP latency 설정"
echo "  Proxy: $PROXY_NAME"
echo "  Latency: ${LATENCY}ms (median)"
echo "  Jitter: ${JITTER}ms (stdev)"
echo ""

# 기존 toxic 제거 (이미 있을 경우)
curl -s -X DELETE "$TOXIPROXY_API/proxies/$PROXY_NAME/toxics/smtp_latency" > /dev/null 2>&1

# latency toxic 추가
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$TOXIPROXY_API/proxies/$PROXY_NAME/toxics" \
  -H "Content-Type: application/json" \
  -d "{
    \"name\": \"smtp_latency\",
    \"type\": \"latency\",
    \"attributes\": {
      \"latency\": $LATENCY,
      \"jitter\": $JITTER
    }
  }")

HTTP_CODE=$(echo "$RESPONSE" | tail -1)
BODY=$(echo "$RESPONSE" | head -n -1)

if [ "$HTTP_CODE" = "200" ]; then
  echo "OK: toxic 설정 완료"
else
  echo "FAIL (HTTP $HTTP_CODE): $BODY"
  exit 1
fi

# 설정 확인
echo ""
echo "현재 proxy 상태:"
curl -s "$TOXIPROXY_API/proxies/$PROXY_NAME" | python -m json.tool 2>/dev/null || \
  curl -s "$TOXIPROXY_API/proxies/$PROXY_NAME"
