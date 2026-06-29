# Changelog

## [0.2.1](https://github.com/JorisJonkers-dev/auth-api/compare/v0.2.0...v0.2.1) (2026-06-29)


### Bug Fixes

* add .dockerignore to exclude build artifacts from image context ([#3](https://github.com/JorisJonkers-dev/auth-api/issues/3)) ([94244a4](https://github.com/JorisJonkers-dev/auth-api/commit/94244a4d59a9a2153e85dd637374aaa9782ac8cf))

## [0.2.0](https://github.com/JorisJonkers-dev/auth-api/compare/v0.1.0...v0.2.0) (2026-06-28)


### Features

* adopt published gradle-conventions plugins ([#627](https://github.com/JorisJonkers-dev/auth-api/issues/627)) ([fd757fc](https://github.com/JorisJonkers-dev/auth-api/commit/fd757fcd46bdd0c293dc0a56c783dc29614b2bc6))
* adopt published kotlin-spring-commons modules; remove local libs/kotlin-common ([#628](https://github.com/JorisJonkers-dev/auth-api/issues/628)) ([a8805d3](https://github.com/JorisJonkers-dev/auth-api/commit/a8805d3405ed10cb8846966376b9cd614f882a31))
* **auth-api:** add AGENTS_LOGIN service permission ([#700](https://github.com/JorisJonkers-dev/auth-api/issues/700)) ([e3a949f](https://github.com/JorisJonkers-dev/auth-api/commit/e3a949f42d6b7b205156082bc3bed1612f6312db))
* **auth-api:** mint X-Agents-Verified-Jwt edge assertion (G2 phase 1) ([#671](https://github.com/JorisJonkers-dev/auth-api/issues/671)) ([57288ff](https://github.com/JorisJonkers-dev/auth-api/commit/57288ff7cef51b4915a1b46a123698227abecb66))
* **auth-api:** native bearer auth via OAuth2 resource server + app-native client (G1) ([#666](https://github.com/JorisJonkers-dev/auth-api/issues/666)) ([63e4a4c](https://github.com/JorisJonkers-dev/auth-api/commit/63e4a4c7e575171be7590a683d412fd1a0b7c37a))
* cut over to ExtraToast/agents published images ([#657](https://github.com/JorisJonkers-dev/auth-api/issues/657)) ([6841aa6](https://github.com/JorisJonkers-dev/auth-api/commit/6841aa6d130f50cca803a87a061d1dc84e01c435))
* fix OAuth2 session auth, enforce 2FA, expand testing ([#40](https://github.com/JorisJonkers-dev/auth-api/issues/40)) ([824ca47](https://github.com/JorisJonkers-dev/auth-api/commit/824ca473fc51262a74ecd472ed282790615a4fc4))
* RBAC for services, admin API, and app-ui dashboard ([#35](https://github.com/JorisJonkers-dev/auth-api/issues/35)) ([4469b05](https://github.com/JorisJonkers-dev/auth-api/commit/4469b053406764a3f12808361d783e94a664629c))


### Bug Fixes

* **app-ui:** repair account page + add My Apps nav + polish admin ([#170](https://github.com/JorisJonkers-dev/auth-api/issues/170)) ([a8b46cc](https://github.com/JorisJonkers-dev/auth-api/commit/a8b46cccd5b76a603cb5fea09a73e8faad28bd62))
* **auth-api:** Lettuce 500ms timeout + HikariCP keepalive ([#172](https://github.com/JorisJonkers-dev/auth-api/issues/172)) ([3c4f4ea](https://github.com/JorisJonkers-dev/auth-api/commit/3c4f4ea30b3b865e7808be610f55d6a6f8576217))
* **rebrand:** publish auth-api under JorisJonkers-dev coordinates ([#1](https://github.com/JorisJonkers-dev/auth-api/issues/1)) ([537607e](https://github.com/JorisJonkers-dev/auth-api/commit/537607e1fadda40d9ad15cad50c17562bd176cf5))


### Performance Improvements

* **auth-api/assistant-api:** kill N+1 on permissions + add hot-path indexes ([#163](https://github.com/JorisJonkers-dev/auth-api/issues/163)) ([fd3af0f](https://github.com/JorisJonkers-dev/auth-api/commit/fd3af0f7c8d69845daf27d3ec9c5c3b9f7ccf427))
* **auth-api:** Valkey-backed @Cacheable for user lookups with explicit eviction ([#164](https://github.com/JorisJonkers-dev/auth-api/issues/164)) ([610b928](https://github.com/JorisJonkers-dev/auth-api/commit/610b9284f087e918e324b82f5acd2e862ba0b0a8))
