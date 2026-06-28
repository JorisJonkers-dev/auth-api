# Auth API Contract

`client-spec/openapi/auth-api.json` is the committed API contract for auth-api consumers.

The contract is produced by `:api:exportOpenApiSpec`, which runs only the JUnit
`contract-export` tag. Regular integration tests exclude that tag so contract
export does not start Postgres, Valkey, or RabbitMQ.

Regenerate the contract before changing controller request or response shapes:

```bash
./gradlew :api:exportOpenApiSpec --no-daemon
git diff -- client-spec/openapi/auth-api.json
```

Generated clients are published from the committed spec:

- npm: `@jorisjonkers-dev/auth-api-client`
- Maven: `dev.jorisjonkers:auth-api-client-java`
- Maven: `dev.jorisjonkers:auth-api-client-kotlin`
