# Agents

Guidance for any coding agent working in this repository.

- Repository contract: [CLAUDE.md](CLAUDE.md)
- Estate-wide conventions: https://github.com/JorisJonkers-dev/workspace/blob/main/CLAUDE.md
- API contract: [CONTRACT.md](CONTRACT.md)

## Build and test

```bash
./gradlew :api:test
./gradlew :api:integrationTest
./gradlew :api:exportOpenApiSpec
```

The exported spec lands in `client-spec/openapi`; `api` holds the service code.
