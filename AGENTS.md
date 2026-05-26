# AGENTS.md

This file provides guidance to Codex (Codex.ai/code) when working with code in this repository.

## Project Overview

芋道 (yudao-cloud) is a Java microservice rapid development platform built on Spring Cloud Alibaba. This is the `master-jdk17` branch running JDK 17 + Spring Boot 3.5.9.

**Core Tech Stack:** Spring Boot 3.5.9, Spring Cloud Alibaba, Nacos (registry/config), MyBatis-Plus, Redis, Flowable (workflow), Lombok, MapStruct.

## Build Commands

```bash
# Full build (skip tests for speed)
mvn clean install -DskipTests

# Build single module
mvn clean install -pl yudao-server -DskipTests

# Run tests for specific module
mvn test -pl yudao-module-system/yudao-module-system-server

# Run single test class
mvn test -pl yudao-module-system/yudao-module-system-server -Dtest=BpmCategoryServiceImplTest
```

## Architecture

The project is a Maven multi-module monorepo with two deployment modes:

**Monolith mode (default):** `yudao-server` is the entry point. It imports module dependencies and runs all services in one JVM. OpenFeign is disabled in this mode.

**Microservice mode:** Each `yudao-module-xxx-server` runs independently, communicating via OpenFeign + Nacos.

### Module Structure

```
yudao-server/              # Single entry point (the "container")
yudao-framework/           # Shared Spring Boot starters
  ├── yudao-common/        # Core DTOs, utils, exceptions
  ├── yudao-spring-boot-starter-mybatis/  # MyBatis-Plus config, tenant, data permission
  ├── yudao-spring-boot-starter-security/ # Auth, token, permission
  ├── yudao-spring-boot-starter-redis/    # Redis cache, distributed locks
  ├── yudao-spring-boot-starter-rpc/      # OpenFeign RPC
  ├── yudao-spring-boot-starter-web/      # Web layer, API encryption, desensitization
  └── ... (mq, job, test, monitor, etc.)
yudao-module-system/       # System management (users, roles, menus, dicts)
yudao-module-infra/        # Infrastructure (code gen, config, file, job)
yudao-module-ai/           # AI/LLM integration
yudao-module-bpm/          # Workflow (Flowable)
yudao-module-crm/          # CRM
yudao-module-erp/          # ERP
yudao-module-mall/         # E-commerce
yudao-module-wms/          # Warehouse management
yudao-module-mes/          # Manufacturing execution
yudao-module-iot/          # IoT
yudao-dependencies/        # BOM (dependency version management)
```

Each business module follows the pattern: `yudao-module-xxx-api/` (DTOs + Feign clients) + `yudao-module-xxx-server/` (implementation).

### Module Activation

Most modules are commented out in `yudao-server/pom.xml` to speed up compilation. Uncomment the dependency to enable a module.

## Key Conventions

- **Package structure:** `cn.iocoder.yudao.module.<module>.<layer>` (controller, service, dal, po)
- **Code style:** Follows Alibaba Java Development Manual. Lombok configured with `accessors.chain=true`, `@SuperBuilder` support via `callsuper=CALL`.
- **SQL scripts:** Located in `sql/<database>/` (supports MySQL, Oracle, PostgreSQL, SQL Server, DM, etc.)
- **Database config:** See `application-local.yaml` in each server module's `src/main/resources/`
- **Config center:** Nacos. Local profile uses `application-local.yaml`.
