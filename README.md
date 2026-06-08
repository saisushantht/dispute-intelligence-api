# Dispute Intelligence API

A backend service for **Bamboo Express** that ingests chargeback (payment dispute) data, computes dispute metrics, detects anomalous segments, and recommends which open disputes are worth fighting — exposed as a documented REST API.

Built with **Java 21 + Spring Boot 4 + PostgreSQL (Supabase) + Flyway**.

---

## Quick Start

**Prerequisites:** Java 21, a PostgreSQL database (this project uses Supabase).

1. Set the database connection as environment variables:

   ```bash
   export SUPABASE_DB_URL="jdbc:postgresql://<host>:5432/postgres?sslmode=require"
   export SUPABASE_DB_USER="<user>"
   export SUPABASE_DB_PASSWORD="<password>"
   ```

2. Run:

   ```bash
   ./mvnw spring-boot:run
   ```

On first startup the app runs the Flyway schema migration and **auto-generates ~12,000 transactions and 330 chargebacks** with realistic planted patterns (see below). On subsequent startups it detects existing data and skips seeding.

3. Open the interactive API docs:

   ```
   http://localhost:8080/swagger-ui.html
   ```

---

## Architecture

Clean layered design with clear separation of concerns:

```
web/         REST controllers (DisputeController, MetricsController, PrioritizationController)
service/     Business logic (DisputeService, MetricsService, PrioritizationService, DataSeeder)
repository/  Spring Data JPA repositories (aggregate queries in JPQL)
domain/      JPA entities (Transaction, Chargeback)
reference/   Reason-code catalog + generator distributions
db/migration Flyway schema (V1__init.sql)
```

**Key design decision — transactions as a denominator.** Chargeback *rate* = disputes / total transactions. Storing only disputes makes a true rate impossible, so the service maintains a separate `transactions` table (~12k rows). Every segmented rate (by category, country, merchant) is a genuine ratio against its own transaction volume, not an approximation.

**Learned win probabilities.** The prioritization engine does not hard-code which reason codes are winnable. It learns win probability per reason code from resolved history (`won / (won + lost)`) and feeds that into the fight score — so the model adapts as real outcomes accumulate.

---

## API Endpoints

### Ingestion & Storage
| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/disputes` | Create a single dispute (auto-fills reason description, generates IDs) |
| POST | `/api/disputes/bulk` | Bulk-create disputes |
| GET | `/api/disputes` | List disputes; filter by `status`, `reasonCode`, `productCategory`, `customerCountry`, `merchantId` |
| GET | `/api/disputes/{id}` | Fetch one dispute |
| PATCH | `/api/disputes/{id}/status` | Update status/outcome (open → won/lost/expired) |

### Metrics & Analysis
| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/metrics` | Global metrics: chargeback rate, win rate, response rate, totals, status breakdown |
| GET | `/api/metrics/segments?dimension=` | Segmented metrics by `reasonCode`, `productCategory`, `customerCountry`, `merchantId`, `reasonCategory` |
| GET | `/api/metrics/anomalies?dimension=&threshold=` | Segments with chargeback rate ≥ `threshold`× the overall rate |

### Prioritization
| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/disputes/prioritized?limit=` | Open disputes ranked by fight score |

---

## The Fight Score

For each **open dispute still within its deadline**:

```
fightScore       = expectedRecovery × urgencyWeight
expectedRecovery = winProbability(reasonCode) × amount
winProbability   = learned from historical won / resolved per reason code
urgencyWeight    = 1.5 (≤2 days left) · 1.3 (≤5) · 1.15 (≤10) · 1.0 (>10)
```

This balances all three required factors: **amount at stake**, **reason-code win probability**, and **deadline pressure**. Expired and closed disputes are excluded because they cannot be represented. Each API response includes the component breakdown so the ranking is fully transparent.

---

## Generated Test Data

Data is generated dynamically on startup (seeded RNG for reproducibility), never hard-coded. It deliberately contains:

- **330 chargebacks** over ~12 months, across 4 statuses (open / won / lost / expired)
- **10 reason codes** with realistic frequency (fraud common, processing errors rare)
- **3 currencies** (USD/EUR/GBP), amounts $15–$800 (most $30–$150)
- **6 product categories, 10 countries, 4 merchants**
- **Handwoven Textiles** at ~2.3× the overall chargeback rate (an injected anomaly)
- Reason code **13.1** with a high win rate (~73%) and **10.4** with a low win rate (~6%)
- **82 expired disputes** (missed the response deadline)
- An injected **fraud ring**: 6 disputes sharing one customer email + IP within a short window

---

## Demo Guide

```bash
# 1. Global health metrics (chargeback rate, win rate, totals)
curl -s localhost:8080/api/metrics

# 2. Anomaly detection — surfaces Handwoven Textiles as 2.3x above average
curl -s "localhost:8080/api/metrics/anomalies?dimension=productCategory"

# 3. Win rate per reason code (13.1 high, 10.4 low)
curl -s "localhost:8080/api/metrics/segments?dimension=reasonCode"

# 4. Top disputes to fight this week, with transparent scoring
curl -s "localhost:8080/api/disputes/prioritized?limit=10"

# 5. Create and then resolve a dispute
curl -s -X POST localhost:8080/api/disputes -H "Content-Type: application/json" \
  -d '{"merchantId":"SELLER_BALI_001","productCategory":"Ceramics & Pottery","customerCountry":"US","customerEmail":"buyer@example.com","customerIp":"203.0.113.10","amount":120.50,"currency":"USD","reasonCode":"13.1"}'
```

---

## Assumptions

- A reviewer can point the app at any empty Postgres database; schema and data bootstrap automatically.
- "Responded" disputes are those resolved to won/lost; response rate = resolved / total.
- Win probability uses reason-code history with a 0.30 prior for codes with no resolved samples yet.
- Anomaly threshold defaults to 2× the overall rate and is configurable per request.

## Tech Stack

Java 21 · Spring Boot 4 · Spring Data JPA / Hibernate · PostgreSQL (Supabase) · Flyway · springdoc-openapi (Swagger UI) · Maven
