import org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED
import org.gradle.api.tasks.testing.logging.TestLogEvent.SKIPPED
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.File.separator
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

    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlin.test.junit5)
    testRuntimeOnly(libs.junit.platform.launcher)

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

    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.assertj.swing)
}

configurations {
    compileOnly { extendsFrom(configurations.annotationProcessor.get()) }
    implementation.configure {
        setOf(
            libs.junit.vintage.engine.get().module.run { group to name }
        ).forEach {
            when {
                it.first.isNotBlank() && it.second.isNotBlank() ->
                    exclude(it.first, it.second)

                else -> exclude(it.first)
            }
        }
    }
}

tasks {
    withType<JavaCompile>().configureEach { options.encoding = UTF_8.name() }
    withType<JavaExec>().configureEach { defaultCharacterEncoding = UTF_8.name() }
    withType<Javadoc>().configureEach { options.encoding = UTF_8.name() }
    withType<Test>().configureEach { defaultCharacterEncoding = UTF_8.name() }
}

tasks.withType<KotlinCompile> {
    compilerOptions.freeCompilerArgs = listOf("-Xjsr305=strict")
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging { events(FAILED, SKIPPED) }
    reports {
        html.required = true
        ignoreFailures = true
    }
}

tasks.register<Delete>("cleanResources") {
    description = "Delete directory build/resources"
    group = "build"
    delete("build${separator}resources")
}

tasks.jacocoTestReport {
    executionData(files("${layout.buildDirectory}${separator}jacoco${separator}test.exec"))
    reports.xml.required = true
}

tasks.register<TestReport>("testReport") {
    description = "Generates an HTML test report from the results of testReport task."
    group = "report"
    "${layout.buildDirectory}${separator}reports${separator}tests"
        .run(::file)
        .run(destinationDirectory::set)
    "test".run(tasks::get)
        .outputs
        .files
        .run(testResults::setFrom)
}

tasks.register<Exec>("apiCheckFirefox") {
    group = "verification"
    description = "Check spring boot project then show report in firefox"
    dependsOn("check")
    commandLine(
        "firefox",
        "--new-tab",
        "build${separator}reports${separator}tests${separator}test${separator}index.html"
            .run(layout.projectDirectory.asFile.toPath()::resolve)
            .toAbsolutePath(),
    )
}