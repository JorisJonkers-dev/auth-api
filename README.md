# auth-api

Kotlin/Spring authentication and authorization API for jorisjonkers.dev.

## What It Is

`auth-api` owns login, session, profile, TOTP, password reset, email
confirmation, OAuth2/OIDC, and forward-auth verification endpoints. It publishes
the OpenAPI contract and generated TypeScript, Java, and Kotlin clients for UI
and service consumers.

## Local Use

```bash
./gradlew :api:test
./gradlew :api:integrationTest
./gradlew :api:exportOpenApiSpec
```

API consumers should use the published contract or generated client rather than
copying internal service code.

## Related

- API contract: [CONTRACT.md](./CONTRACT.md)
- OpenAPI spec: [client-spec/openapi/auth-api.json](./client-spec/openapi/auth-api.json)
- Client/UI: [auth-ui](https://github.com/JorisJonkers-dev/auth-ui)

## Links

- [Organization profile](https://github.com/JorisJonkers-dev)
- [Security policy](https://github.com/JorisJonkers-dev/.github/security/policy)
- [Changelog](./CHANGELOG.md)
- [License](./LICENSE)

Copyright (c) Joris Jonkers. Source available for viewing only; use, copying,
modification, redistribution, deployment, or reuse is not licensed. See
[LICENSE](./LICENSE).
