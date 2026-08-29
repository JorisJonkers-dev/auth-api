# Changelog

## [0.6.0](https://github.com/JorisJonkers-dev/auth-api/compare/v0.5.2...v0.6.0) (2026-08-29)


### Features

* **auth:** add HERMES service permission ([#47](https://github.com/JorisJonkers-dev/auth-api/issues/47)) ([5b414fc](https://github.com/JorisJonkers-dev/auth-api/commit/5b414fcbfbe324f3811105fe4d54f1cee25dea37))
* **auth:** register hermes as an OIDC client ([#49](https://github.com/JorisJonkers-dev/auth-api/issues/49)) ([6275e33](https://github.com/JorisJonkers-dev/auth-api/commit/6275e3362a9cb1f9cfbaade4dbb09ee91afe9117))

## [0.5.2](https://github.com/JorisJonkers-dev/auth-api/compare/v0.5.1...v0.5.2) (2026-08-27)


### Bug Fixes

* **redis:** give the reconnect handshake room to complete ([#45](https://github.com/JorisJonkers-dev/auth-api/issues/45)) ([6cd5d0e](https://github.com/JorisJonkers-dev/auth-api/commit/6cd5d0e03470160649e35b80b809a67f660dfe2b))

## [0.5.1](https://github.com/JorisJonkers-dev/auth-api/compare/v0.5.0...v0.5.1) (2026-08-27)


### Bug Fixes

* **auth:** do not let a broker outage break registration ([#36](https://github.com/JorisJonkers-dev/auth-api/issues/36)) ([fabb8d8](https://github.com/JorisJonkers-dev/auth-api/commit/fabb8d8d6af8672de31aca964b34c02beea9a391))
* **email:** require EmailService so mail cannot fail silently ([#40](https://github.com/JorisJonkers-dev/auth-api/issues/40)) ([11efc65](https://github.com/JorisJonkers-dev/auth-api/commit/11efc651949b9122a4914cf8fc2df950900ae187))
* remove an accidentally committed .claude gitlink ([#41](https://github.com/JorisJonkers-dev/auth-api/issues/41)) ([1071472](https://github.com/JorisJonkers-dev/auth-api/commit/107147245a42e747ca3a3f6b8b1235a428a108e6))

## [0.5.0](https://github.com/JorisJonkers-dev/auth-api/compare/v0.4.1...v0.5.0) (2026-08-26)


### Features

* **auth:** register Outline as an OIDC client ([#34](https://github.com/JorisJonkers-dev/auth-api/issues/34)) ([f7b1a84](https://github.com/JorisJonkers-dev/auth-api/commit/f7b1a849065a35a2d3c3f0485ad8b852951abbd3))


### Bug Fixes

* **platform:** make render-local.sh able to run ([#24](https://github.com/JorisJonkers-dev/auth-api/issues/24)) ([317656b](https://github.com/JorisJonkers-dev/auth-api/commit/317656b535f85b2a838ece22e27bdee025bb05c9))

## [0.4.1](https://github.com/JorisJonkers-dev/auth-api/compare/v0.4.0...v0.4.1) (2026-08-20)


### Bug Fixes

* **vault:** add Kubernetes authentication for the Vault session ([#14](https://github.com/JorisJonkers-dev/auth-api/issues/14)) ([b36da1a](https://github.com/JorisJonkers-dev/auth-api/commit/b36da1aba1a70d349506306d0fbeac9e1a583003))

## [0.4.0](https://github.com/JorisJonkers-dev/auth-api/compare/v0.3.2...v0.4.0) (2026-08-20)


### Features

* **ci:** publish images for arm64 as well as amd64 ([#20](https://github.com/JorisJonkers-dev/auth-api/issues/20)) ([3874753](https://github.com/JorisJonkers-dev/auth-api/commit/387475385c2289332bd6e29389717e582096b33e))

## [0.3.2](https://github.com/JorisJonkers-dev/auth-api/compare/v0.3.1...v0.3.2) (2026-08-19)


### Bug Fixes

* **ci:** bump the reusable workflow pins so job timeouts apply ([#18](https://github.com/JorisJonkers-dev/auth-api/issues/18)) ([05d3a51](https://github.com/JorisJonkers-dev/auth-api/commit/05d3a511028cd9d38ae382c64e19b68595437024))

## [0.3.1](https://github.com/JorisJonkers-dev/auth-api/compare/v0.3.0...v0.3.1) (2026-07-10)


### Bug Fixes

* **ci:** add workflow_dispatch to publish workflow for manual republish ([cce7eda](https://github.com/JorisJonkers-dev/auth-api/commit/cce7eda052d783e38aab9892e2f6144f2b151084))

## [0.3.0](https://github.com/JorisJonkers-dev/auth-api/compare/v0.2.1...v0.3.0) (2026-07-10)


### Features

* **platform:** adopt deploy-platform v2 ([e070194](https://github.com/JorisJonkers-dev/auth-api/commit/e07019478b9669d86f1d9b5499128af3d6362870))

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
