# Repository Guidelines

## Project Structure & Module Organization
This repository is a Kotlin/Ktor backend service. The main source lives under `backend/src/main/kotlin` with the entry point in `Application.kt` and routes in `Routing.kt`. Runtime configuration and logging are in `backend/src/main/resources` (`application.yaml`, `logback.xml`). Gradle build logic is in `backend/build.gradle.kts`. There is no `src/test` yet; add tests under `backend/src/test/kotlin` when you introduce them.

## Build, Test, and Development Commands
Run commands from the `backend` directory:
- `./gradlew run` starts the Ktor server on `http://0.0.0.0:8080`.
- `./gradlew build` compiles and packages the app.
- `./gradlew test` runs the unit tests (when present).
- `./gradlew buildFatJar` builds a standalone JAR.
- `./gradlew buildImage` builds a Docker image, and `./gradlew runDocker` runs it.

## Coding Style & Naming Conventions
Use Kotlin standard style with 4-space indentation. Package names are lowercase (`com.battmon`), classes use `PascalCase`, and functions/variables use `camelCase`. Keep routes and module wiring small and readable; prefer clear handler names over inline lambdas when logic grows. No formatter/linter is configured yet, so rely on IDE defaults (IntelliJ) or Kotlin style guides.

## Testing Guidelines
Tests should live in `backend/src/test/kotlin` and use `kotlin-test-junit` with Ktor’s test host. Name test classes after the unit under test (e.g., `RoutingTest`) and use descriptive test method names. Run `./gradlew test` before submitting changes that affect behavior.

## Commit & Pull Request Guidelines
Git history currently contains only “Initial commit,” so no convention is established. Use short, descriptive commit messages (e.g., “Add health check route”). For PRs, include a clear description, note any configuration changes, and add screenshots/log output only when behavior changes are visible (e.g., startup logs or API responses).

## Configuration & Runtime Tips
Edit `backend/src/main/resources/application.yaml` for server settings and `backend/src/main/resources/logback.xml` for logging. When adding new routes or modules, register them in `Application.module()` to keep startup behavior explicit.
