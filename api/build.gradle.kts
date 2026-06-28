plugins {
    alias(libs.plugins.jorisjonkers.spring)
    alias(libs.plugins.jorisjonkers.detekt)
    alias(libs.plugins.jorisjonkers.ktlint)
    alias(libs.plugins.jorisjonkers.testing)
    alias(libs.plugins.jorisjonkers.jooq.codegen)
}

jooqCodegen {
    schemaName.set("PUBLIC")
    packageName.set("com.jorisjonkers.personalstack.auth.jooq")
    migrationLocations.set(listOf("filesystem:src/main/resources/db/migration"))
}

dependencies {
    implementation(libs.kotlin.commons.command)
    implementation(libs.kotlin.commons.crac)
    implementation(libs.kotlin.commons.email)
    implementation(libs.kotlin.commons.events)
    implementation(libs.kotlin.commons.exceptions)
    implementation(libs.kotlin.commons.messaging)
    implementation(libs.kotlin.commons.observability)
    implementation(libs.kotlin.commons.timing)
    implementation(libs.kotlin.commons.vault)
    implementation(libs.kotlin.commons.web)
    testImplementation(libs.kotlin.commons.archunit.test)
    testImplementation(libs.kotlin.commons.test.support)
    // Enables the wildcard tracing aspect in kotlin-commons-observability — without
    // spring-aop the @Aspect bean isn't proxied and the advice never
    // fires. Spring Boot 4 dropped the `spring-boot-starter-aop`
    // shortcut, so pull the underlying jars directly.
    implementation("org.springframework:spring-aop")
    implementation("org.aspectj:aspectjweaver:1.9.25.1")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-authorization-server")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.session:spring-session-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-amqp")
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.3")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("org.springframework.boot:spring-boot-starter-jooq")
    implementation("org.jooq:jooq")
    implementation("tools.jackson.module:jackson-module-kotlin:3.1.4")
    runtimeOnly("org.postgresql:postgresql")
    // Tracing runtime jars. With these on the classpath Spring Boot
    // activates micrometer-tracing + the OTLP exporter and starts shipping
    // spans to Alloy → Tempo; MDC traceId/spanId begin populating the JSON
    // log lines so Loki↔Tempo correlation works.
    runtimeOnly("io.micrometer:micrometer-tracing-bridge-otel")
    runtimeOnly("io.opentelemetry:opentelemetry-exporter-otlp")
    implementation("dev.turingcomplete:kotlin-onetimepassword:3.0.0")
    implementation("commons-codec:commons-codec")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.testcontainers:testcontainers-postgresql")
    testImplementation("org.testcontainers:testcontainers-rabbitmq")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
}

tasks.named<Test>("integrationTest") {
    useJUnitPlatform {
        includeTags("integration")
        excludeTags("contract-export")
    }
}

tasks
    .matching { it.name.startsWith("runKtlint") && it.name.endsWith("OverMainSourceSet") }
    .configureEach {
        dependsOn("generateJooq")
    }

tasks.register<Test>("exportOpenApiSpec") {
    description = "Exports the OpenAPI spec to client-spec/openapi/auth-api.json from a springdoc MVC slice"
    group = "documentation"
    testClassesDirs = sourceSets["integrationTest"].output.classesDirs
    classpath = sourceSets["integrationTest"].runtimeClasspath
    extensions.configure<org.gradle.testing.jacoco.plugins.JacocoTaskExtension> {
        isEnabled = false
    }
    useJUnitPlatform {
        includeTags("contract-export")
    }
    systemProperty(
        "openapi.spec.output",
        rootProject.layout.projectDirectory
            .file("client-spec/openapi/auth-api.json")
            .asFile.absolutePath,
    )
    outputs.upToDateWhen { false }
}
