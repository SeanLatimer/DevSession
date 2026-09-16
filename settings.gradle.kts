pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/") { name = "FabricMC" }
        maven("https://maven.neoforged.net/releases/") { name = "NeoForged" }
        maven("https://maven.kikugie.dev/releases") { name = "KikuGie Releases" }
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.9.8"

    // Selects fabric-loom vs fabric-loom-remap based on the Minecraft version,
    // so obfuscated (<26.1) and unobfuscated (>=26.1) builds share one script
    id("dev.kikugie.loom-back-compat") version "0.4.2"

    // Provisions missing JDK toolchains (17/21/25 per node)
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

stonecutter {
    create(rootProject) {
        /**
         * Creates version nodes for multiple loaders as `versions/{project}-{loader}`,
         * each assigned the per-loader build script `build.{loader}.gradle.kts`.
         */
        fun match(project: String, vararg loaders: String, version: String = project) {
            for (loader in loaders) version("$project-$loader", version).buildscript("build.$loader.gradle.kts")
        }

        // NeoForge does not exist for 1.20.1 (it started at 1.20.2)
        match("1.20.1", "fabric")
        match("1.20.4", "neoforge")
        match("1.21.1", "fabric", "neoforge")
        match("26.1.2", "fabric", "neoforge")
        vcsVersion = "1.21.1-fabric"
    }
}

rootProject.name = "DevSession"
