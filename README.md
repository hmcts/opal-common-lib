# opal-commonlib

Shared Opal components for authentication, authorisation, and other cross-cutting concerns.
This module builds a reusable Java library—there is no runnable Spring Boot application here.

## Build & Test

Use the bundled Gradle wrapper:

```bash
./gradlew clean build
```

The task runs the JUnit 5 test suite and produces `build/libs/opal-common-lib-<version>.jar`.
During day-to-day work the library is usually brought into `opal-fines-service` via Gradle composite build (`includeBuild('../opal-common-lib')` in settings.gradle), which gives instant feedback in IntelliJ.

## Local Development Tips

- Library source lives under `src/main/java/uk/gov/hmcts/opal/common/...`; unit tests live in `src/test/java/uk/gov/hmcts/opal/common/...`.
- Additional source sets (`functionalTest`, `integrationTest`, `smokeTest`) are configured and available if extra coverage is helpful.
- Dependency versions are managed through the Spring Boot and Spring Cloud BOMs declared in `build.gradle`.
- If you prefer testing the published coordinates instead of `includeBuild`, install the jar to your local Maven cache:
  ```bash
  ./gradlew publishToMavenLocal
  ```
  Then depend on `uk.gov.hmcts:opal-common-lib:<version>` from the consuming project.

## Publishing

Azure Artifacts publishing is handled by the CI pipeline once pull requests merge; no local publish step is required when developing.

## License

This project is licensed under the MIT License – see [LICENSE](LICENSE) for details.
