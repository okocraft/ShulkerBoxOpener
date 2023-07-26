plugins {
    java
    id("io.papermc.paperweight.userdev") version "1.5.5"
}

java.toolchain.languageVersion.set(JavaLanguageVersion.of(17))

group = "net.okocraft"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    paperweight.foliaDevBundle("1.20.1-R0.1-SNAPSHOT")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks {
    reobfJar {
        outputJar.set(
                project.layout.buildDirectory
                        .file("libs/ShulkerBoxOpener-${project.version}.jar")
        )
    }

    build {
        dependsOn(reobfJar)
    }

    compileJava {
        options.encoding = Charsets.UTF_8.name()
        options.release.set(17)
    }

    processResources {
        filteringCharset = Charsets.UTF_8.name()

        filesMatching(listOf("plugin.yml")) {
            expand("projectVersion" to project.version)
        }
    }
}
