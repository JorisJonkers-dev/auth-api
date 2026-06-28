plugins {
    alias(libs.plugins.jorisjonkers.openapi.client)
    `maven-publish`
}

openApiClient {
    specPath.set("client-spec/openapi/auth-api.json")
    apiPackage.set("dev.jorisjonkers.auth.client.api")
    modelPackage.set("dev.jorisjonkers.auth.client.model")
    packageName.set("dev.jorisjonkers.auth.client")
    generatorName.set("java")
    library.set("restclient")
}

java {
    withSourcesJar()
    withJavadocJar()
}

tasks.named("sourcesJar") {
    dependsOn(tasks.named("generate"))
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            groupId = "dev.jorisjonkers"
            artifactId = "auth-api-client-java"
            version = project.version.toString()
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/JorisJonkers-dev/auth-api")
            credentials {
                username = providers.environmentVariable("GITHUB_ACTOR").orNull
                password = providers.environmentVariable("GITHUB_TOKEN").orNull
            }
        }
    }
}
