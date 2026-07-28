#!/usr/bin/env bash
# start all retail services locally in the distributed profile against docker-compose infra
set -euo pipefail
cd "$(dirname "$0")/.."

export PATH="/opt/homebrew/bin:$PATH"

# build once so spring-boot:run is fast
mvn -q -f services/pom.xml -DskipTests install

run() { (cd "services/$1" && mvn -q spring-boot:run -Dspring-boot.run.profiles=distributed) & }

run pricing-service
run loyalty-service
run inventory-service
run payment-simulator
run checkout-service

echo "services starting: checkout 8082, pricing 8081, loyalty 8083, inventory 8084, payment 8085"
echo "swagger at http://localhost:<port>/swagger-ui.html"
wait
