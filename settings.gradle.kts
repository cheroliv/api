pluginManagement {
    repositories {
        mavenLocal()
        google()
        mavenCentral()
        gradlePluginPortal()
        setOf(
            "https://plugins.gradle.org/m2/",
            "https://mvnrepository.com/repos/springio-plugins-release",
            "https://maven.xillio.com/artifactory/libs-release/",
            "https://archiva-repository.apache.org/archiva/repository/public/",
        ).forEach(::maven)
    }
    plugins {
        kotlin("jvm").version(extra["kotlin.version"].toString())
        kotlin("plugin.serialization").version(extra["kotlin.version"].toString())
        kotlin("plugin.allopen").version(extra["kotlin.version"].toString())
        kotlin("plugin.noarg").version(extra["kotlin.version"].toString())
        kotlin("plugin.spring").version(extra["kotlin.version"].toString())
        id("org.springframework.boot").version(extra["springboot.version"].toString())
        id("io.spring.dependency-management").version(extra["spring_dependency_management.version"].toString())
        id("com.github.ben-manes.versions").version(extra["deps.version"].toString())
//        id("org.jbake.site").version(extra["jbake-gradle.version"].toString())
//        id("com.github.node-gradle.node").version(extra["node-gradle.version"].toString())
//        id("org.gradle.toolchains.foojay-resolver-convention") version ("0.8.0")
//        id("org.asciidoctor:asciidoctor-gradle-jvm-slides") version (extra["asciidoctor.gradle.version"].toString())
//        id("org.asciidoctor:asciidoctor-gradle-base") version (extra["asciidoctor.gradle.version"].toString())
//        id("org.asciidoctor:asciidoctor-gradle-jvm-gems") version (extra["asciidoctor.gradle.version"].toString())
    }
}

dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        mavenLocal()
        google()
        mavenCentral()
        setOf(
            "https://plugins.gradle.org/m2/",
            "https://mvnrepository.com/repos/springio-plugins-release",
            "https://maven.xillio.com/artifactory/libs-release/",
            "https://archiva-repository.apache.org/archiva/repository/public/"
        ).forEach(::maven)
    }
}
rootProject.name = "api"