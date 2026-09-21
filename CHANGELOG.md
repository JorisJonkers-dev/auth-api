# Changelog

## [0.10.0](https://github.com/JorisJonkers-dev/auth-api/compare/v0.9.1...v0.10.0) (2026-09-21)


### Features

* **ci:** tell fleet-infra when an image is published ([#73](https://github.com/JorisJonkers-dev/auth-api/issues/73)) ([634a05f](https://github.com/JorisJonkers-dev/auth-api/commit/634a05fba14052ecc9a9a390066fc178f0789951))


### Bug Fixes

* **auth:** apply the configured session timeout ([#72](https://github.com/JorisJonkers-dev/auth-api/issues/72)) ([de97952](https://github.com/JorisJonkers-dev/auth-api/commit/de97952636adb2a22241f3bf1c40862f396b9f0c))

## [0.9.1](https://github.com/JorisJonkers-dev/auth-api/compare/v0.9.0...v0.9.1) (2026-09-21)


### Bug Fixes

* **auth:** stop an unreadable session from locking sign-in out ([#70](https://github.com/JorisJonkers-dev/auth-api/issues/70)) ([ef7c52e](https://github.com/JorisJonkers-dev/auth-api/commit/ef7c52ea00b36122c6aa90af7d57131b76f76d45))

## [0.9.0](https://github.com/JorisJonkers-dev/auth-api/compare/v0.8.1...v0.9.0) (2026-09-21)


### Features

* **auth:** declare the error contract as problem+json ([#68](https://github.com/JorisJonkers-dev/auth-api/issues/68)) ([f3c44df](https://github.com/JorisJonkers-dev/auth-api/commit/f3c44dfa72eebc16c4e348ab3aebcf3bbc3de658))

## [0.8.1](https://github.com/JorisJonkers-dev/auth-api/compare/v0.8.0...v0.8.1) (2026-09-21)


### Bug Fixes

* **auth:** make cache eviction visible to the next read ([#65](https://github.com/JorisJonkers-dev/auth-api/issues/65)) ([7560254](https://github.com/JorisJonkers-dev/auth-api/commit/75602541e9c22106dc7575b3a8135ef13c07b220))

## [0.8.0](https://github.com/JorisJonkers-dev/auth-api/compare/v0.7.1...v0.8.0) (2026-09-21)


### Features

* **auth:** per-host service tokens for headless forward-auth access ([#63](https://github.com/JorisJonkers-dev/auth-api/issues/63)) ([b811430](https://github.com/JorisJonkers-dev/auth-api/commit/b811430111462387decd566d8c93aec9c1e77470))
* **auth:** sign in with a username or an email address ([#60](https://github.com/JorisJonkers-dev/auth-api/issues/60)) ([0ca1058](https://github.com/JorisJonkers-dev/auth-api/commit/0ca10580ca29bec9f691f6750a9824b67471c1e2))


### Bug Fixes

* **auth:** declare bearerAuth per operation, not for the whole document ([#61](https://github.com/JorisJonkers-dev/auth-api/issues/61)) ([fcb65ed](https://github.com/JorisJonkers-dev/auth-api/commit/fcb65edeb476edb944a83f65d8668b144ff2e937))
* **auth:** keep a stale bearer token from blocking sign-in ([#57](https://github.com/JorisJonkers-dev/auth-api/issues/57)) ([16544bb](https://github.com/JorisJonkers-dev/auth-api/commit/16544bbaf27dc4378cd5224f3926c72826fd0357))
* **auth:** redirect to login from authorize whatever the Accept header ([#59](https://github.com/JorisJonkers-dev/auth-api/issues/59)) ([6d1e670](https://github.com/JorisJonkers-dev/auth-api/commit/6d1e67019f24f57983536cb28f186932940c9950))

## [0.7.1](https://github.com/JorisJonkers-dev/auth-api/compare/v0.7.0...v0.7.1) (2026-09-19)


### Bug Fixes

* **email:** make the password reset link configurable ([#56](https://github.com/JorisJonkers-dev/auth-api/issues/56)) ([b6d8372](https://github.com/JorisJonkers-dev/auth-api/commit/b6d83726315e8257b6f03765e504d86def7ae214))

## [0.7.0](https://github.com/JorisJonkers-dev/auth-api/compare/v0.6.2...v0.7.0) (2026-09-08)


### Features

* **auth:** gate overleaf behind a service permission ([#54](https://github.com/JorisJonkers-dev/auth-api/issues/54)) ([33250c9](https://github.com/JorisJonkers-dev/auth-api/commit/33250c99103228129e4e56df8dcffae765632dc2))

## [0.6.2](https://github.com/JorisJonkers-dev/auth-api/compare/v0.6.1...v0.6.2) (2026-08-30)


### Bug Fixes

* **auth:** register the JWK Set endpoint filter ([#52](https://github.com/JorisJonkers-dev/auth-api/issues/52)) ([72970b1](https://github.com/JorisJonkers-dev/auth-api/commit/72970b1d1fed2b39ed04afe733fc5d1c9ffbf51d))

## [0.6.1](https://github.com/JorisJonkers-dev/auth-api/compare/v0.6.0...v0.6.1) (2026-08-30)


### Bug Fixes

* **auth:** make the JWK Set endpoint publicly readable ([#50](https://github.com/JorisJonkers-dev/auth-api/issues/50)) ([6d6e230](https://github.com/JorisJonkers-dev/auth-api/commit/6d6e23053b9bd6d97d0184f98680beee624086e8))

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
