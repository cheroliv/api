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
include("installer")