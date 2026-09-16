import org.gradle.api.JavaVersion
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.jvm.toolchain.JvmVendorSpec

plugins {
    // This plugin applies the correct loom variant based on the Minecraft version
    id("dev.kikugie.loom-back-compat")
}

version = "${property("mod.version")}+${sc.current.version}"
base.archivesName = "${property("mod.id") as String}-fabric"

val requiredJava: JavaVersion = when {
    sc.current.parsed >= "26.1" -> JavaVersion.VERSION_25
    sc.current.parsed >= "1.20.5" -> JavaVersion.VERSION_21
    else -> JavaVersion.VERSION_17
}

dependencies {
    // Not provided by minecraft, bundled into the jar
    implementation("com.electronwill.night-config:core:3.6.5")
    implementation("com.electronwill.night-config:toml:3.6.5")
    include("com.electronwill.night-config:core:3.6.5")
    include("com.electronwill.night-config:toml:3.6.5")

    // OS credential store access, bundled into the jar
    val keyringDeps = arrayOf(
        "com.github.javakeyring:java-keyring:${property("deps.keyring")}",
        "net.java.dev.jna:jna:${property("deps.jna")}",
        "net.java.dev.jna:jna-platform:${property("deps.jna")}",
        "pt.davidafsilva.apple:jkeychain:${property("deps.jkeychain")}",
        "de.swiesend:secret-service:${property("deps.secret_service")}",
        "com.github.hypfvieh:dbus-java-core:${property("deps.dbus_java")}",
        "com.github.hypfvieh:dbus-java-transport-native-unixsocket:${property("deps.dbus_java")}",
        "at.favre.lib:hkdf:${property("deps.hkdf")}",
        "org.slf4j:slf4j-api:${property("deps.slf4j")}",
    )
    for (dep in keyringDeps) {
        implementation(dep)
        include(dep)
    }

    minecraft("com.mojang:minecraft:${sc.current.version}")
    loomx.applyMojangMappings()
    modImplementation("net.fabricmc:fabric-loader:${property("deps.fabric_loader")}")
}

loom {
    runConfigs {
        named("client") {
            ideConfigGenerated(true)
            runDirectory = rootProject.file("run")
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

        filesMatching("fabric.mod.json") { expand(props) }

        val mixinJava = "JAVA_${requiredJava.majorVersion}"
        filesMatching("mixins.devsession.json") { expand("java" to mixinJava) }

        exclude("META-INF/neoforge.mods.toml", "META-INF/mods.toml")

        from(rootProject.file("LICENSE")) { rename { "LICENSE_DevSession.txt" } }
        from(rootProject.file("branding/logo128x.png")) { rename { "assets/devsession/logo.png" } }
    }

    withType<AbstractArchiveTask> {
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
    }

    register<Copy>("buildAndCollect") {
        group = "build"
        description = "Copies built mod jars to the root build/distributions directory"
        from(loomx.modJar.flatMap { it.archiveFile }, loomx.modSourcesJar.flatMap { it.archiveFile })
        into(rootProject.layout.buildDirectory.dir("distributions"))
    }

    named("assemble") { dependsOn("buildAndCollect") }
}
