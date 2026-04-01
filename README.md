# Prüfstein

**Device compliance management for ISO 27001 — powered by [osquery](https://osquery.io/).**

Prüfstein lets you define compliance checks as SQL queries, run them on every employee's device via a lightweight local agent, and track pass/fail status across your entire fleet from a central web dashboard.

---

## How it works

The web app (Quarkus) lets admins define Compliance Items: each item belongs to a ComplianceGroup (e.g. "A.8 Asset Management") and contains a SQL query and a JEXL pass/fail expression. The dashboard shows per-user/device compliance Reports, each containing a ComplianceResult per item.

A lightweight local agent runs on each employee's machine. It fetches all ComplianceItems via `GET /api/checks`, executes each SQL query through osquery, evaluates the result against the expected expression, and POSTs a Report back via `POST /api/reports`. The agent runs periodically via cron, launchd, or systemd.

---

## Domain model

| Entity | Key fields | Purpose |
|---|---|---|
| `AppUser` | firstname, lastname, mail | An employee whose devices are checked |
| `ComplianceGroup` | name | Groups related items (maps to ISO 27001 control family) |
| `ComplianceItem` | name, query, expectedExpression, group | One osquery check with a JEXL pass/fail expression |
| `Report` | user, deviceHostname, checkedAt, passed | One agent run for one device |
| `ComplianceResult` | item, report, actualResult (JSON), passed | Outcome of one check in one report |

---

## Compliance Items

A Compliance Item is a SQL query against the [osquery schema](https://www.osquery.io/schema/) plus a **[JEXL](https://commons.apache.org/proper/commons-jexl/) expression** that is evaluated against the full JSON result array. The expression must return `true` for the check to pass.

The JEXL context exposes `results` — the parsed JSON array returned by `osqueryi --json`.

**Example — disk encryption (macOS):**
```sql
SELECT encrypted FROM mounts WHERE path = '/';
```
```jexl
results[0].encrypted == "1"
```

**Example — firewall enabled (macOS):**
```sql
SELECT global_state FROM alf;
```
```jexl
results[0].global_state == "1"
```

**Example — screen lock timeout ≤ 300 s:**
```sql
SELECT value FROM preferences WHERE domain = 'com.apple.screensaver' AND key = 'idleTime';
```
```jexl
results[0].value =~ '\d+' && Integer.parseInt(results[0].value) <= 300
```

**Example — no unknown listening ports:**
```sql
SELECT DISTINCT port FROM listening_ports WHERE pid != 0;
```
```jexl
results.size() == 0
```

---

## Local agent

The agent is a Java program installed on each employee's machine that:

1. Authenticates to the web app via **OIDC** (the employee logs in once; the agent uses the token)
2. Downloads the current list of `ComplianceItem`s
3. Runs each query via `osqueryi --json`
4. Evaluates the **JEXL expression** (`expectedExpression`) against the full JSON result
5. Sends a `Report` payload back to the web app

One user can have multiple devices — each device reports independently and appears as a separate `Report` identified by hostname.

It is intended to run as a scheduled task (launchd on macOS, systemd on Linux, Task Scheduler on Windows).

---

## Web app stack

- **Quarkus 3** + Renarde (server-side MVC)
- **Qute** templates
- **Hibernate Panache** + PostgreSQL (Dev Services in dev mode)
- **Quarkus Web Bundler** (Tailwind CSS + Alpine.js, no Node.js required)

### Running locally

```bash
cd web
./mvnw quarkus:dev
```

Quarkus Dev Services starts a PostgreSQL container automatically. The app is available at `http://localhost:8080`.

---

## ISO 27001 mapping

Compliance Groups map to ISO 27001 Annex A control families. Suggested groups:

| Group | Controls |
|---|---|
| A.8 Asset Management | Inventory, software install policy |
| A.9 Access Control | Screen lock, password policy, MFA presence |
| A.10 Cryptography | Disk encryption |
| A.12 Operations Security | Auto-update, AV, firewall |
| A.13 Network Security | VPN, DNS-over-HTTPS |

---

## Architecture decisions

| Topic | Decision |
|---|---|
| Local agent | Java (single distributable JAR) |
| Authentication | OIDC — employee logs in once, token stored by the agent |
| Pass/fail logic | JEXL expression evaluated against full osquery JSON output |
| Result payload | Full JSON string from `osqueryi --json` stored as `actualResult` |
| Multi-device | One `AppUser` → many `Report`s, differentiated by `deviceHostname` |
