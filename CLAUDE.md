# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Battery monitoring system with a Kotlin/Ktor backend. The backend is a minimal REST API service using Ktor framework with Netty engine, configured via YAML.

## Development Commands

All commands run from the `backend` directory:

```bash
./gradlew run          # Start server on http://0.0.0.0:8080
./gradlew build        # Compile and package
./gradlew test         # Run tests
./gradlew buildFatJar  # Build standalone JAR
./gradlew buildImage   # Build Docker image
./gradlew runDocker    # Run with Docker
```

## Architecture

### Backend Structure
- **Entry point**: `backend/src/main/kotlin/Application.kt` - Contains `main()` and `Application.module()`
- **Routing**: `backend/src/main/kotlin/Routing.kt` - All route definitions via `Application.configureRouting()`
- **Configuration**: `backend/src/main/resources/application.yaml` - Server port and module configuration
- **Logging**: `backend/src/main/resources/logback.xml`

### Key Patterns
- The server uses Ktor's `EngineMain` approach with YAML configuration
- All modules/plugins are registered in `Application.module()`
- Routes are organized via extension functions on `Application`
- Main package: `com.battmon`

### Testing
- Tests belong in `backend/src/test/kotlin/` (currently empty)
- Use `kotlin-test-junit` with Ktor's test host
- Test class naming: match the file under test (e.g., `RoutingTest`)

## Configuration Notes

- Server configuration lives in `application.yaml` (port, modules)
- Module registration in YAML: `com.battmon.ApplicationKt.module`
- Default port: 8080
- When adding routes, register configuration functions in `Application.module()`
