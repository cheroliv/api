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
            "https://archiva-repository.apache.org/archiva/repository/public/"
        ).forEach(::maven)
    }
    plugins {
//    id(libs.plugins.`kotlin-jvm`.get().id) version libs.versions.kotlin
//    id(libs.plugins.`kotlin-spring`.get().id) version libs.versions.kotlin
//    id(libs.plugins.`kotlin-allopen`.get().id) version libs.versions.kotlin
//    id(libs.plugins.`kotlin-noarg`.get().id) version libs.versions.kotlin
//    id(libs.plugins.`kotlin-serialization`.get().id) version libs.versions.kotlin
//    id(libs.plugins.`spring-boot`.get().id) version libs.versions.springboot
//    id(libs.plugins.`dependency-management`.get().id) version libs.versions.`dependency-management`
//    id(libs.plugins.versions.get().id) version libs.versions.versions
        kotlin("plugin.serialization").version(extra["kotlin.version"].toString())
        kotlin("plugin.allopen").version(extra["kotlin.version"].toString())
        kotlin("plugin.noarg").version(extra["kotlin.version"].toString())
        kotlin("plugin.spring").version(extra["kotlin.version"].toString())
        id("org.springframework.boot").version(extra["springboot.version"].toString())
        id("io.spring.dependency-management").version(extra["spring_dependency_management.version"].toString())
        id("com.github.ben-manes.versions").version(extra["deps.version"].toString())
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