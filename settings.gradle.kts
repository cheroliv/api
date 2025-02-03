pluginManagement {
    repositories {
        mavenLocal()
        google()
        mavenCentral()
        gradlePluginPortal()
        maven("https://plugins.gradle.org/m2/")
        maven("https://maven.xillio.com/artifactory/libs-release/")
        maven("https://mvnrepository.com/repos/springio-plugins-release")
        maven("https://archiva-repository.apache.org/archiva/repository/public/")
    }
    plugins {
        id("org.jbake.site").version(extra["jbake-gradle.version"].toString())
        id("com.github.node-gradle.node").version(extra["node-gradle.version"].toString())
//        id("org.gradle.toolchains.foojay-resolver-convention") version ("0.8.0")
//        id("org.asciidoctor:asciidoctor-gradle-jvm-slides") version (extra["asciidoctor.gradle.version"].toString())
//        id("org.asciidoctor:asciidoctor-gradle-base") version (extra["asciidoctor.gradle.version"].toString())
//        id("org.asciidoctor:asciidoctor-gradle-jvm-gems") version (extra["asciidoctor.gradle.version"].toString())
        kotlin("jvm").version(extra["kotlin.version"].toString())
        kotlin("plugin.serialization").version(extra["kotlin.version"].toString())
        kotlin("plugin.allopen").version(extra["kotlin.version"].toString())
        kotlin("plugin.noarg").version(extra["kotlin.version"].toString())
        kotlin("plugin.spring").version(extra["kotlin.version"].toString())
        id("org.springframework.boot").version(extra["springboot.version"].toString())
        id("io.spring.dependency-management").version(extra["spring_dependency_management.version"].toString())
        id( "com.github.ben-manes.versions"). version( extra["deps.version"].toString())
    }
}

dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        mavenLocal()
        google()
        mavenCentral()
        maven("https://maven.xillio.com/artifactory/libs-release/")
        maven("https://mvnrepository.com/repos/springio-plugins-release")
        maven("https://archiva-repository.apache.org/archiva/repository/public/")
        maven ("https://plugins.gradle.org/m2/")
    }
    versionCatalogs {
        create("libs") {
            from(files("gradle/libs.versions.toml"))
        }
    }
}
rootProject.name = "api"