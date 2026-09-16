import org.gradle.api.JavaVersion
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.jvm.toolchain.JvmVendorSpec
import org.gradle.jvm.tasks.Jar

plugins {
    id("net.neoforged.moddev")
    id("neoforge-mutex")
    `maven-publish`
}

version = "${property("mod.version")}+${sc.current.version}"
base.archivesName = "${property("mod.id") as String}-neoforge"
group = "dev.silentsean.mod.devsession"

val neoLoader = property("deps.neo_loader") as String

val requiredJava: JavaVersion = when {
    sc.current.parsed >= "26.1" -> JavaVersion.VERSION_25
    sc.current.parsed >= "1.20.5" -> JavaVersion.VERSION_21
    else -> JavaVersion.VERSION_17
}

dependencies {
    // Provided by minecraft (FML bundles night-config), compile-only
    compileOnly("com.electronwill.night-config:core:3.6.5")
    compileOnly("com.electronwill.night-config:toml:3.6.5")

    // OS credential store access, bundled into the jar via jar-in-jar.
    // slf4j and JNA are intentionally omitted: FML provides them, and duplicates
    // of either conflict with the versions minecraft strictly pins
    val keyringDeps = linkedMapOf(
        "com.github.javakeyring:java-keyring" to "${property("deps.keyring")}",
        "pt.davidafsilva.apple:jkeychain" to "${property("deps.jkeychain")}",
        "de.swiesend:secret-service" to "${property("deps.secret_service")}",
        "com.github.hypfvieh:dbus-java-core" to "${property("deps.dbus_java")}",
        "com.github.hypfvieh:dbus-java-transport-native-unixsocket" to "${property("deps.dbus_java")}",
        "at.favre.lib:hkdf" to "${property("deps.hkdf")}",
    )
    for ((notation, version) in keyringDeps) {
        val dep = add("implementation", "$notation:$version") as ModuleDependency
        val jarJarDep = add("jarJar", "$notation:$version") as ExternalModuleDependency
        jarJarDep.version {
            strictly("[$version]")
            prefer(version)
        }
        if (notation == "com.github.javakeyring:java-keyring") {
            dep.exclude(mapOf("group" to "net.java.dev.jna"))
            jarJarDep.exclude(mapOf("group" to "net.java.dev.jna"))
        }
    }

    // Embedded libraries are only loaded in dev runs via the additional runtime classpath before MC 1.21.9
    afterEvaluate {
        if (sc.current.parsed < "26.1") {
            for ((notation, version) in keyringDeps) {
                add("additionalRuntimeClasspath", "$notation:$version")
            }
        }
    }
}

neoForge {
    version = neoLoader

    mods {
        register("DevSession") {
            sourceSet(sourceSets.main.get())
        }
    }

    runs {
        register("client") {
            gameDirectory = rootProject.file("run")
            client()
        }
    }
}

java {
    withSourcesJar()
    sourceCompatibility = requiredJava
    targetCompatibility = requiredJava

    toolchain {
        vendor = JvmVendorSpec.ADOPTIUM
        languageVersion = JavaLanguageVersion.of(requiredJava.majorVersion)
    }
}

tasks {
    processResources {
        fun MutableMap<String, String>.register(key: String, property: String) {
            val value: String = sc.properties[property]
            inputs.property(key, value)
            set(key, value)
        }

        val props = buildMap {
            register("id", "mod.id")
            register("name", "mod.name")
            register("version", "mod.version")
            register("minecraft", "mod.mc_compat")
        }

        filesMatching("META-INF/*mods.toml") { expand(props) }

        val mixinJava = "JAVA_${requiredJava.majorVersion}"
        filesMatching("mixins.devsession.json") { expand("java" to mixinJava) }

        exclude("fabric.mod.json")

        from(rootProject.file("LICENSE")) { rename { "LICENSE_DevSession.txt" } }
        from(rootProject.file("branding/logo128x.png")) { rename { "assets/devsession/logo.png" } }
    }

    named("createMinecraftArtifacts") {
        dependsOn("stonecutterGenerate")
    }

    withType<AbstractArchiveTask> {
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
    }

    register<Copy>("buildAndCollect") {
        group = "build"
        description = "Copies built mod jars to the root build/distributions directory"
        from(jar.flatMap { it.archiveFile }, named<Jar>("sourcesJar").flatMap { it.archiveFile })
        into(rootProject.layout.buildDirectory.dir("distributions"))
    }

    named("assemble") { dependsOn("buildAndCollect") }
}

publishing {
    publications {
        register<MavenPublication>("mod") {
            artifactId = "${property("mod.id") as String}-neoforge"
            artifact(project.tasks.named("jar"))
            artifact(project.tasks.named("sourcesJar"))

            pom {
                name = "DevSession (NeoForge)"
                description = "Safely authenticate Minecraft accounts in development environments."
                url = "https://github.com/SeanLatimer/DevSession"
                licenses {
                    license {
                        name = "MIT"
                        url = "https://opensource.org/licenses/MIT"
                    }
                }
            }
        }
    }

    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/SeanLatimer/DevSession")
            credentials {
                username = System.getenv("GITHUB_ACTOR") ?: (findProperty("gpr.user") as String?)
                password = System.getenv("GITHUB_TOKEN") ?: (findProperty("gpr.key") as String?)
            }
        }
    }
}
