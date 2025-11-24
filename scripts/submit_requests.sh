#!/bin/bash

echo "Submitting 200 finance requests..."

for i in {1..200}; do
  # Generate random amount between 100 and 10000
  amount=$(( (RANDOM % 9901) + 100 ))
  
  curl -s -X POST http://localhost:8084/api/finance/approve \
    -H "Content-Type: application/json" \
    -d "{
      \"customerId\": \"LOAD-TEST-${i}\",
      \"amount\": ${amount}.00,
      \"purpose\": \"Load test request ${i} with amount ${amount}\"
    }" > /dev/null
  
  # Print progress every 20 requests
  if [ $((i % 20)) -eq 0 ]; then
    echo "Submitted ${i}/200 requests..."
  fi
done

echo "✅ All 200 requests submitted!"
echo ""
echo "Checking queue stats..."
sleep 2
curl -s http://localhost:8084/api/finance/queue-stats | jq '.data'
