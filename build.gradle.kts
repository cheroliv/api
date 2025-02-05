import Build_gradle.Application.API
import Build_gradle.Application.CLI
import Build_gradle.Application.INSTALLER
import Build_gradle.Application.SQL_SCHEMA
import org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED
import org.gradle.api.tasks.testing.logging.TestLogEvent.SKIPPED
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.springframework.boot.gradle.tasks.run.BootRun
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
    `java-library`
    setOf(
        libs.plugins.kotlin.jvm to libs.versions.kotlin,
        libs.plugins.kotlin.spring to libs.versions.kotlin,
        libs.plugins.kotlin.allopen to libs.versions.kotlin,
        libs.plugins.kotlin.noarg to libs.versions.kotlin,
        libs.plugins.kotlin.serialization to libs.versions.kotlin,
        libs.plugins.spring.boot to libs.versions.springboot,
        libs.plugins.spring.dependency.management to libs.versions.spring.dependency.management,
        libs.plugins.versions to libs.versions.deps.versions,
    ).forEach { id(it.first.get().pluginId).version(it.second) }
}

object Application {
    const val API = "app.API"
    const val CLI = "app.CommandLine"
    const val INSTALLER = "app.workspace.Installer"
    const val SQL_SCHEMA = "app.users.api.dao.DatabaseConfiguration"
}

INSTALLER.run(application.mainClass::set)
API.run(springBoot.mainClass::set)
val mockitoAgent = configurations.create("mockitoAgent")

repositories {
    mavenCentral()
    setOf(
        "https://maven.repository.redhat.com/ga/",
        "https://repo.spring.io/milestone",
        "https://repo.spring.io/snapshot",
        "https://maven.pkg.jetbrains.space/kotlin/p/kotlin/bootstrap/"
    ).forEach(::maven)
}

dependencyManagement.imports {
    libs.versions.springboot.get()
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
        @Suppress("UnstableApiUsage")
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
    withType<JavaCompile>().configureEach { options.encoding = UTF_8.name() }
    withType<JavaExec>().configureEach { defaultCharacterEncoding = UTF_8.name() }
    withType<Javadoc>().configureEach { options.encoding = UTF_8.name() }
    withType<Test>().configureEach { defaultCharacterEncoding = UTF_8.name() }
    withType<BootRun>().configureEach { defaultCharacterEncoding = UTF_8.name() }
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
    INSTALLER.run(mainClass::set)
    "main".run(sourceSets::get)
        .runtimeClasspath
        .run(::setClasspath)
}

tasks.named<BootRun>("bootRun") {
    API.run(mainClass::set)
    "main".run(sourceSets::get)
        .runtimeClasspath
        .run(::setClasspath)
}

tasks.register<JavaExec>("cli") {
    group = "application"
    description = "Run CLI application: ./gradlew cli -Pargs=--gui"
    CLI.run(mainClass::set)
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
    SQL_SCHEMA.run(mainClass::set)
    "main".run(sourceSets::get)
        .runtimeClasspath
        .run(::setClasspath)
}