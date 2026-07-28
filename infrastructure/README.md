# infrastructure

## bring up backing services
```
docker compose -f infrastructure/docker-compose.yml up -d
```
starts postgres (5432), redis (6379), redpanda (9092) and the redpanda console (8080).

## run the retail services
```
bash infrastructure/run-services.sh
```
builds the maven reactor and launches all five services in the `distributed` profile so they call
each other over http and talk to postgres. ports: checkout 8082, pricing 8081, loyalty 8083,
inventory 8084, payment 8085. each exposes swagger at `/swagger-ui.html`.

For a quick demo you can also run checkout on its own (default profile) — it falls back to in-process
pricing/payment clients and does not need the other services or docker.

## frontends
- POS: `cd apps/pos-web && npm install && npm run dev` (proxies /api to the services)
- dashboard: `cd apps/agent-dashboard && npm install && npm run dev`
