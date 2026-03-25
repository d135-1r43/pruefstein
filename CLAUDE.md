# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Layout

The active Quarkus application lives in the `web/` subdirectory. The root-level Java sources have been removed and replaced by this subdirectory structure.

```
web/          ← Quarkus Maven project (pruefstein-web)
  pom.xml
  src/main/java/
    com/pruefstein/   ← Infrastructure (health check, example REST resource)
    model/            ← Domain models (Todo)
    rest/             ← Renarde controllers
    util/             ← Qute template extensions, dev-mode startup seeding
  src/main/resources/
    templates/        ← Qute server-side templates (extend main.html)
    web/              ← Web Bundler assets (app.js, app.scss → auto-bundled)
    application.properties
  src/test/java/
    com/pruefstein/   ← @QuarkusTest unit tests, @QuarkusIntegrationTest IT tests
```

## Commands

All Maven commands must be run from the `web/` directory.

```bash
# Dev mode with live reload
./mvnw quarkus:dev

# Run unit tests
./mvnw test

# Run a single test class
./mvnw test -Dtest=GreetingResourceTest

# Build (skip ITs by default)
./mvnw package

# Build and run integration tests against packaged jar
./mvnw verify -DskipITs=false

# Native build (requires GraalVM)
./mvnw package -Pnative
```

## Architecture

**Stack**: Quarkus 3.32.4 · Java 25 · Renarde (server-side MVC) · Qute templates · Hibernate Panache Next · PostgreSQL · Quarkus Web Bundler

### Request flow

HTTP request → Renarde `Controller` subclass (in `rest/`) → Qute template (in `templates/`) → rendered HTML.

Controllers use `@CheckedTemplate` inner classes for type-safe template binding. Templates extend `main.html` via `{#include main.html}`.

### Frontend bundling

Assets under `src/main/resources/web/` are automatically bundled by Quarkus Web Bundler (zero-config, no Node.js required). SCSS is compiled, JS is bundled — use standard imports.

### Persistence

`model/Todo.java` currently uses in-memory stubs (real Hibernate/Panache persistence is commented out). PostgreSQL JDBC driver is on the classpath but `application.properties` has no datasource configured yet. Quarkus Dev Services will spin up a PostgreSQL container automatically in dev/test mode when no datasource URL is set.

### Dev-mode seeding

`util/Startup.java` is `@ApplicationScoped` and seeds sample Todo data only when running in `@io.quarkus.runtime.LaunchMode.DEVELOPMENT`.

### Health / OpenAPI

- Health endpoint: `/q/health` (SmallRye Health, `@Liveness` in `com.pruefstein.MyLivenessCheck`)
- Swagger UI: `/q/swagger-ui` (SmallRye OpenAPI, available in dev mode)