plugins {
    java
    id("io.papermc.paperweight.userdev") version "1.7.1"
}

java.toolchain.languageVersion.set(JavaLanguageVersion.of(21))

group = "net.okocraft"
version = "1.0.0"
val mcVersion = "1.21"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    paperweight.paperDevBundle("$mcVersion-R0.1-SNAPSHOT")
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

paperweight.reobfArtifactConfiguration = io.papermc.paperweight.userdev.ReobfArtifactConfiguration.MOJANG_PRODUCTION

tasks {
    compileJava {
        options.encoding = Charsets.UTF_8.name()
        options.release.set(21)
    }

    processResources {
        filteringCharset = Charsets.UTF_8.name()

        filesMatching(listOf("plugin.yml")) {
            expand("projectVersion" to project.version, "mcVersion" to mcVersion)
        }
    }

    jar {
        archiveFileName = "ShulkerBoxOpener-${project.version}-mc$mcVersion.jar"
    }
}
