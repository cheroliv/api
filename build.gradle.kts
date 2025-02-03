@file:Suppress(
    "UnstableApiUsage",
    "ConstPropertyName",
    "VulnerableLibrariesLocal",
    "RedundantSuppression"
)

import Build_gradle.Constants.commonsIoVersion
import Build_gradle.Constants.jacksonVersion
import Build_gradle.Constants.jgitVersion
import Build_gradle.Constants.langchain4jVersion
import org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED
import org.gradle.api.tasks.testing.logging.TestLogEvent.SKIPPED
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.springframework.boot.gradle.tasks.run.BootRun
import java.nio.file.FileSystems

buildscript {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        google()
        maven { url = uri("https://plugins.gradle.org/m2/") }
    }
    dependencies { classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.1.10") }
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
extra["springShellVersion"] = "3.3.3"

object Constants {
    const val langchain4jVersion = "0.36.2"
    const val testcontainersVersion = "1.20.1"

    //    const val asciidoctorGradleVersion = "4.0.0-alpha.1"
    const val commonsIoVersion = "2.13.0"
    const val jacksonVersion = "2.17.2"//2.18.0
    const val arrowKtVersion = "1.2.4"
    const val jgitVersion = "6.10.0.202406032230-r"
    const val apiVersion = "0.0.1"
    const val BLANK = ""
}

val sep: String get() = FileSystems.getDefault().separator

data class DockerHub(
    val username: String = properties["docker_hub_login"].toString(),
    val password: String = properties["docker_hub_password"].toString(),
    val token: String = properties["docker_hub_login_token"].toString(),
    val email: String = properties["docker_hub_email"].toString(),
    val image: String = "cheroliv/e3po"
)

val mockitoAgent = configurations.create("mockitoAgent")

repositories {
    mavenCentral()
    maven("https://maven.repository.redhat.com/ga/")
    maven("https://repo.spring.io/milestone")
    maven("https://repo.spring.io/snapshot")
    maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/bootstrap/")
}

//dependencyManagement { imports { mavenBom("org.springframework.shell:spring-shell-dependencies:${property("springShellVersion")}") } }
dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:${properties["springboot.version"]}")
    }
}

dependencies {
    testImplementation(libs.assertj.swing)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlin.test.junit5)
    testRuntimeOnly(libs.junit.platform.launcher)

    implementation(libs.commons.beanutils)
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

    // Jackson marshaller
    implementation(libs.jackson.module.kotlin)
    implementation(libs.jackson.module.jsonSchema)
    implementation(libs.jackson.dataformat.yaml)
    implementation(libs.jackson.datatype.jsr310)

    // JGit
    implementation(libs.jgit.core)
    implementation(libs.jgit.archive)
    implementation(libs.jgit.ssh)

    // Kotlin
    implementation(libs.kotlin.reflect)
    implementation(libs.kotlin.stdlib.jdk8)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)

    // Spring tools
    developmentOnly(libs.spring.boot.devtools)
    annotationProcessor(libs.spring.boot.configuration.processor)
    runtimeOnly(libs.spring.boot.properties.migrator)
    //developmentOnly("org.springframework.boot:spring-boot-docker-compose")

    // Springboot
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.boot.starter.mail)
    implementation(libs.spring.boot.starter.thymeleaf)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.webflux)
    implementation(libs.spring.boot.starter.data.r2dbc)

    implementation(libs.spring.boot.starter.test) {
        exclude(module = libs.mockito.core.get().module.name)
    }

    // Spring security
    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.security.data)
    testImplementation(libs.spring.security.test)

    // Spring cloud
    testImplementation(libs.spring.cloud.starter.contract.verifier){
        exclude(module = "commons-collections")
    }

    // Spring AOP
//    testImplementation("org.springframework.boot:spring-boot-starter-aop")

    // Spring tests
    // Spring-Shell
//    implementation("org.springframework.shell:spring-shell-starter")
//    testImplementation("org.springframework.shell:spring-shell-starter-test")

    // JWT
    implementation(libs.jjwt.impl)//"io.jsonwebtoken:jjwt-impl:${properties["jsonwebtoken.version"]}")
    implementation(libs.jjwt.jackson)//"io.jsonwebtoken:jjwt-jackson:${properties["jsonwebtoken.version"]}")

    // SSL
    implementation(libs.netty.tcnative.boringssl.static)//"io.netty:netty-tcnative-boringssl-static:${properties["boring_ssl.version"]}")

    // Database
    runtimeOnly(libs.r2dbc.postgresql)//"org.postgresql:r2dbc-postgresql:${properties["r2dbc-postgresql.version"]}")

    // Kotlin-JUnit5
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlin.test.junit5)

    // Mock
    testImplementation(libs.mockito.core)
    mockitoAgent(libs.mockito.core) { isTransitive = false }
    testImplementation("org.mockito.kotlin:mockito-kotlin:${properties["mockito_kotlin_version"]}")
    testImplementation("org.mockito:mockito-junit-jupiter:${properties["mockito_jupiter.version"]}")
//    testImplementation("io.mockk:mockk:${properties["mockk.version"]}")
//    testImplementation("org.wiremock:wiremock:${properties["wiremock.version"]}") {
//        exclude(module = "commons-fileupload")
//    }
//    testImplementation("com.ninja-squad:springmockk:${properties["springmockk.version"]}")
    testImplementation(libs.commons.fileupload)

    // Archunit
//    testImplementation("com.tngtech.archunit:archunit-junit5-api:${properties["archunit_junit5.version"]}")
//    testRuntimeOnly("com.tngtech.archunit:archunit-junit5-engine:${properties["archunit_junit5.version"]}")

    // Langchain4j
    implementation("dev.langchain4j:langchain4j:$langchain4jVersion")
    implementation("dev.langchain4j:langchain4j-reactor:${properties["langchain4j.version"]}")
    implementation("dev.langchain4j:langchain4j-spring-boot-starter:${properties["langchain4j.version"]}")
    implementation("dev.langchain4j:langchain4j-ollama-spring-boot-starter:${properties["langchain4j.version"]}")
    implementation("dev.langchain4j:langchain4j-hugging-face:${properties["langchain4j.version"]}")
    implementation("dev.langchain4j:langchain4j-mistral-ai:${properties["langchain4j.version"]}")
    implementation("dev.langchain4j:langchain4j-web-search-engine-google-custom:${properties["langchain4j.version"]}")
    implementation("dev.langchain4j:langchain4j-google-ai-gemini:${properties["langchain4j.version"]}")
    implementation("dev.langchain4j:langchain4j-pgvector:${properties["langchain4j.version"]}")
//    implementation("dev.langchain4j:langchain4j-document-parser-apache-pdfbox:${properties["langchain4j.version"]}")
//    implementation("dev.langchain4j:langchain4j-easy-rag:${properties["langchain4j.version"]}")
//    implementation("dev.langchain4j:langchain4j-vertex-ai-gemini-spring-boot-starter:${properties["langchain4j.version"]}")
//    implementation("dev.langchain4j:langchain4j-vertex-ai:${properties["langchain4j.version"]}")
//    implementation("dev.langchain4j:langchain4j-vertex-ai-gemini:${properties["langchain4j.version"]}")
    testImplementation("dev.langchain4j:langchain4j-spring-boot-tests:${properties["langchain4j.version"]}")

    // Arrow-kt
    implementation("io.arrow-kt:arrow-core:${properties["arrow-kt.version"]}")
    implementation("io.arrow-kt:arrow-fx-coroutines:${properties["arrow-kt.version"]}")
    implementation("io.arrow-kt:arrow-integrations-jackson-module:${properties["arrow-kt_jackson.version"]}")


    // Testcontainers
//    testImplementation("org.testcontainers:junit-jupiter")
//    testImplementation("org.testcontainers:postgresql")
//    implementation("org.testcontainers:testcontainers:$testcontainersVersion")
//    implementation("org.testcontainers:ollama:$testcontainersVersion")

    // Reactor
    implementation(libs.reactor.kotlin.extensions)
    implementation(libs.kotlinx.coroutines.reactor)
    testImplementation(libs.reactor.test)

    // misc
    implementation(libs.commons.lang3)
    testImplementation(libs.commons.collections4)
}

configurations {
    compileOnly { extendsFrom(configurations.annotationProcessor.get()) }
    implementation.configure {
        setOf(
            "org.junit.vintage" to "junit-vintage-engine",
            "org.springframework.boot" to "spring-boot-starter-tomcat",
            "org.apache.tomcat" to null
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
    delete("build${sep}resources")
}

tasks.jacocoTestReport {
    executionData(files("${layout.buildDirectory}${sep}jacoco${sep}test.exec"))
    reports.xml.required = true
}

tasks.register<TestReport>("testReport") {
    description = "Generates an HTML test report from the results of testReport task."
    group = "report"
    "${layout.buildDirectory}${sep}reports${sep}tests"
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
        "build${sep}reports${sep}tests${sep}test${sep}index.html"
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


