// Route dependency resolution through the packages proxy inside a docker build.
// The token comes from the BuildKit secret `packages-netrc` (a netrc file with
// `password <key>`). Without that file this script does nothing, so a plain
// local `docker build` keeps resolving from the declared repositories.
// First-party GitHub Packages and file repositories stay as they are; every
// other Maven repository has its URL rewritten to the proxy's equivalent.
val netrcFile = java.io.File(System.getenv("PACKAGES_NETRC") ?: "/run/secrets/packages-netrc")
val proxyToken: String? = if (netrcFile.isFile) {
    netrcFile.readLines().firstNotNullOfOrNull { Regex("""^\s*password\s+(\S+)""").find(it)?.groupValues?.get(1) }
} else null

val proxy = "https://packages.jorisjonkers.dev/maven/"

fun proxyUrlFor(url: java.net.URI): String? {
    val host = url.host ?: return null
    if (url.scheme == "file" || host == "maven.pkg.github.com" || host == "packages.jorisjonkers.dev") return null
    val path = url.path ?: ""
    return when {
        host == "repo.spring.io" && path.startsWith("/milestone") -> proxy + "spring-milestone/"
        host.endsWith("sonatype.org") || host == "central.sonatype.com" && path.contains("snapshots") -> proxy + "sonatype-snapshots/"
        host == "maven.google.com" || host == "dl.google.com" -> proxy + "google-maven/"
        else -> proxy // Maven Central, the Gradle Plugin Portal, and any other mirror of Central
    }
}

fun RepositoryHandler.usePackagesProxy() {
    val token = proxyToken ?: return
    all {
        val repo = this as? MavenArtifactRepository ?: return@all
        val target = runCatching { proxyUrlFor(repo.url) }.getOrNull() ?: return@all
        repo.setUrl(target)
        repo.credentials(HttpHeaderCredentials::class) {
            name = "Authorization"
            value = "Bearer $token"
        }
        repo.authentication {
            create<HttpHeaderAuthentication>("header")
        }
    }
}

settingsEvaluated {
    pluginManagement.repositories.usePackagesProxy()
    dependencyResolutionManagement.repositories.usePackagesProxy()
}

beforeProject {
    buildscript.repositories.usePackagesProxy()
    repositories.usePackagesProxy()
}
