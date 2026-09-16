plugins {
    id("dev.kikugie.stonecutter")
    id("net.neoforged.moddev") version "2.0.147" apply false
}

stonecutter active "1.21.1-neoforge"

stonecutter parameters {
    val (version, loader) = current.project.split('-', limit = 2)

    // Makes version- and loader-specific sections of `stonecutter.properties.toml`
    // apply to the matching node, i.e. [neoforge."1.21.1"]
    properties {
        tags(version, loader)
    }

    // Adds loader constants for `//? if fabric { }` conditions
    constants {
        match(loader, "fabric", "neoforge")
    }
}
