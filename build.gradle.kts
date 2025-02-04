@file:Suppress(
    "UnstableApiUsage",
    "ConstPropertyName",
    "VulnerableLibrariesLocal",
    "RedundantSuppression"
)

import org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED
import org.gradle.api.tasks.testing.logging.TestLogEvent.SKIPPED
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.springframework.boot.gradle.tasks.run.BootRun
import java.io.File.separator


buildscript {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        google()
        maven { url = uri("https://plugins.gradle.org/m2/") }
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
    `java-library`
    kotlin("jvm")
    kotlin("plugin.spring")
    kotlin("plugin.allopen")
    kotlin("plugin.noarg")
    kotlin("plugin.serialization")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    id("com.github.ben-manes.versions")
}

"app.workspace.Installer".run(application.mainClass::set)
"app.API".run(springBoot.mainClass::set)

group = properties["artifact.group"].toString()
version = "0.0.1"

val mockitoAgent = configurations.create("mockitoAgent")

repositories {
    mavenCentral()
    maven("https://maven.repository.redhat.com/ga/")
    maven("https://repo.spring.io/milestone")
    maven("https://repo.spring.io/snapshot")
    maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/bootstrap/")
}

dependencyManagement.imports {
    properties["springboot.version"]
        .run { "org.springframework.boot:spring-boot-dependencies:$this" }
        .run(::mavenBom)
}

dependencies {
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

    implementation(libs.reactor.kotlin.extensions)
    implementation(libs.kotlinx.coroutines.reactor)
    testImplementation(libs.reactor.test)

    implementation(libs.commons.beanutils)
    implementation(libs.commons.lang3)
    testImplementation(libs.commons.collections4)

    implementation(libs.google.api.services.forms)
    implementation(libs.google.api.services.drive)
    implementation(libs.google.api.client.jackson2)
    implementation(libs.google.auth.library.oauth2.http)

    implementation(libs.xz)
    implementation(libs.poi.ooxml)
    implementation(libs.asciidoctorj.diagram)
    implementation(libs.okhttp.digest)
    implementation(libs.grolifant)
    implementation(libs.commons.io)

    implementation(libs.jackson.module.kotlin)
    implementation(libs.jackson.module.jsonSchema)
    implementation(libs.jackson.dataformat.yaml)
    implementation(libs.jackson.datatype.jsr310)

    implementation(libs.jgit.core)
    implementation(libs.jgit.archive)
    implementation(libs.jgit.ssh)

    developmentOnly(libs.spring.boot.devtools)
    annotationProcessor(libs.spring.boot.configuration.processor)
    runtimeOnly(libs.spring.boot.properties.migrator)
    //developmentOnly("org.springframework.boot:spring-boot-docker-compose")
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.boot.starter.mail)
    implementation(libs.spring.boot.starter.thymeleaf)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.webflux)
    implementation(libs.spring.boot.starter.data.r2dbc)
    runtimeOnly(libs.r2dbc.postgresql)


    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.security.data)
    testImplementation(libs.spring.security.test)

    testImplementation(libs.spring.cloud.starter.contract.verifier) {
        exclude(module = libs.commons.collections.obsolete.get().module.name)
    }

    implementation(libs.jjwt.impl)
    implementation(libs.jjwt.jackson)
    implementation(libs.netty.tcnative.boringssl.static)

    implementation(libs.spring.boot.starter.test) {
        exclude(module = libs.mockito.core.get().module.name)
    }
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.assertj.swing)
    testImplementation(libs.mockito.core.apply {
        mockitoAgent(this) { isTransitive = false }
    })
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.mockito.junit.jupiter)
//    testImplementation("org.springframework.boot:spring-boot-starter-aop")
//    testImplementation("org.wiremock:wiremock:${properties["wiremock.version"]}") {
//        exclude(module = "commons-fileupload")
//    }
    testImplementation(libs.commons.fileupload)
//    testImplementation("io.mockk:mockk:${properties["mockk.version"]}")
//    testImplementation("com.ninja-squad:springmockk:${properties["springmockk.version"]}")
    // Testcontainers
//    const val testcontainersVersion = "1.20.1"
//    testImplementation("org.testcontainers:junit-jupiter")
//    testImplementation("org.testcontainers:postgresql")
//    implementation("org.testcontainers:testcontainers:$testcontainersVersion")
//    implementation("org.testcontainers:ollama:$testcontainersVersion")
    // Archunit
//    testImplementation("com.tngtech.archunit:archunit-junit5-api:${properties["archunit_junit5.version"]}")
//    testRuntimeOnly("com.tngtech.archunit:archunit-junit5-engine:${properties["archunit_junit5.version"]}")
    implementation(libs.langchain4j.core)
    implementation(libs.langchain4j.reactor)
    implementation(libs.langchain4j.spring.boot.starter)
    implementation(libs.langchain4j.ollama.spring.boot.starter)
    implementation(libs.langchain4j.hugging.face)
    implementation(libs.langchain4j.mistral.ai)
    implementation(libs.langchain4j.web.search.engine.google.custom)
    implementation(libs.langchain4j.google.ai.gemini)
    implementation(libs.langchain4j.pgvector)
    testImplementation(libs.langchain4j.spring.boot.tests)
//    implementation("dev.langchain4j:langchain4j-document-parser-apache-pdfbox:${properties["langchain4j.version"]}")
//    implementation("dev.langchain4j:langchain4j-easy-rag:${properties["langchain4j.version"]}")
//    implementation("dev.langchain4j:langchain4j-vertex-ai-gemini-spring-boot-starter:${properties["langchain4j.version"]}")
//    implementation("dev.langchain4j:langchain4j-vertex-ai:${properties["langchain4j.version"]}")
//    implementation("dev.langchain4j:langchain4j-vertex-ai-gemini:${properties["langchain4j.version"]}")
}

configurations {
    compileOnly { extendsFrom(configurations.annotationProcessor.get()) }
    implementation.configure {
        setOf(
            libs.spring.boot.starter.tomcat.get().module.run { group to name },
            libs.tomcat.servlet.api.get().module.group to null,
            libs.junit.vintage.engine.get().module.run { group to name }
        ).forEach {
            when {
                it.first.isNotBlank() && it.second?.isNotBlank() == true ->
                    exclude(it.first, it.second)

                else -> exclude(it.first)
            }
        }
    }
}

"node_modules"
    .run(::listOf)
    .toTypedArray()
    .run(::files)
    .run(idea.module.excludeDirs::plusAssign)

tasks {
    withType<JavaCompile>().configureEach { options.encoding = "UTF-8" }
    withType<JavaExec>().configureEach { defaultCharacterEncoding = "UTF-8" }
    withType<Javadoc>().configureEach { options.encoding = "UTF-8" }
    withType<Test>().configureEach { defaultCharacterEncoding = "UTF-8" }
    withType<BootRun>().configureEach { defaultCharacterEncoding = "UTF-8" }
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
    jvmArgs("-javaagent:${mockitoAgent.asPath}")
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


tasks.register<JavaExec>("runWorkspaceInstaller") {
    group = "application"
    description = "Runs the Swing application"
    "app.workspace.Installer".run(mainClass::set)
    "main".run(sourceSets::get)
        .runtimeClasspath
        .run(::setClasspath)
}

tasks.named<BootRun>("bootRun") {
    "app.API".run(mainClass::set)
    "main".run(sourceSets::get)
        .runtimeClasspath
        .run(::setClasspath)
}

tasks.register<JavaExec>("cli") {
    group = "application"
    description = "Run CLI application: ./gradlew cli -Pargs=--gui"
    "app.CommandLine".run(mainClass::set)
    "main".run(sourceSets::get).runtimeClasspath.run(::setClasspath)
    when {
        "args".run(project::hasProperty) -> {
            args = "args"
                .run(project::property)
                .toString()
                .trim()
                .split(" ")
                .filter(String::isNotEmpty)
                .also { "Passing args to CLI: $it".run(logger::info) }
        }
    }
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

tasks.register<JavaExec>("displayCreateTestDbSchema") {
    group = "application"
    description = "Display SQL script who creates database tables into test schema."
    "app.users.api.dao.DatabaseConfiguration".run(mainClass::set)
    "main".run(sourceSets::get)
        .runtimeClasspath
        .run(::setClasspath)
}