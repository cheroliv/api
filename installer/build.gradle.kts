import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import kotlin.text.Charsets.UTF_8

buildscript {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        google()
        maven("https://plugins.gradle.org/m2/")
    }
    dependencies {
        extra["kotlinVersion"] = "2.1.10"
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${extra["kotlinVersion"]}")
    }
}

plugins {
    idea
    jacoco
    application
    setOf(
        libs.plugins.kotlin.jvm to libs.versions.kotlin,
        libs.plugins.kotlin.allopen to libs.versions.kotlin,
        libs.plugins.kotlin.noarg to libs.versions.kotlin,
        libs.plugins.kotlin.serialization to libs.versions.kotlin,
        libs.plugins.versions to libs.versions.deps.versions,
    ).forEach { id(it.first.get().pluginId).version(it.second) }
}

"app.workspace.Installer".run(application.mainClass::set)

repositories {
    mavenCentral()
    setOf(
        "https://maven.repository.redhat.com/ga/",
        "https://maven.pkg.jetbrains.space/kotlin/p/kotlin/bootstrap/"
    ).forEach(::maven)
}

dependencies {
    implementation(libs.commons.lang3)
    implementation(libs.kotlin.reflect)
    implementation(libs.kotlin.stdlib.jdk8)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)


    implementation(libs.arrow.core)
    implementation(libs.arrow.fx.coroutines)
    implementation(libs.arrow.integrations.jackson.module)

    implementation(libs.commons.beanutils)
    implementation(libs.commons.lang3)
    testImplementation(libs.commons.collections4)

    implementation(libs.xz)
    implementation(libs.commons.io)

    implementation(libs.jackson.module.kotlin)
    implementation(libs.jackson.module.jsonSchema)
    implementation(libs.jackson.dataformat.yaml)
    implementation(libs.jackson.datatype.jsr310)

}

configurations {
    compileOnly { extendsFrom(configurations.annotationProcessor.get()) }
}

tasks {
    withType<JavaCompile>().configureEach { options.encoding = UTF_8.name() }
    withType<JavaExec>().configureEach { defaultCharacterEncoding = UTF_8.name() }
    withType<Javadoc>().configureEach { options.encoding = UTF_8.name() }
}

tasks.withType<KotlinCompile> {
    compilerOptions.freeCompilerArgs = listOf("-Xjsr305=strict")
}
