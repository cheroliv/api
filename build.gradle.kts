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
    dependencies { classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.0.21") }
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
}

"app.workspace.Installer".run(application.mainClass::set)
"app.API".run(springBoot.mainClass::set)

group = properties["artifact.group"].toString()
version = "0.0.1"
extra["springShellVersion"] = "3.3.3"
object Constants {
    const val langchain4jVersion = "0.36.2"
    const val testcontainersVersion = "1.20.1"
    const val asciidoctorGradleVersion = "4.0.0-alpha.1"
    const val commonsIoVersion = "2.13.0"
    const val jacksonVersion = "2.17.2"//2.18.0
    const val arrowKtVersion = "1.2.4"
    const val jgitVersion = "6.10.0.202406032230-r"
    const val apiVersion = "0.0.1"
    const val USER_HOME_KEY = "user.home"
    const val BLANK = ""
}

val Project.sep: String get() = FileSystems.getDefault().separator


data class DockerHub(
    val username: String = properties["docker_hub_login"].toString(),
    val password: String = properties["docker_hub_password"].toString(),
    val token: String = properties["docker_hub_login_token"].toString(),
    val email: String = properties["docker_hub_email"].toString(),
    val image: String = "cheroliv/e3po"
)

repositories {
    mavenCentral()
    maven("https://maven.repository.redhat.com/ga/")
    maven("https://repo.spring.io/milestone")
    maven("https://repo.spring.io/snapshot")
    maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/bootstrap/")
}

//dependencyManagement {
//    imports { mavenBom("org.springframework.shell:spring-shell-dependencies:${property("springShellVersion")}") }
//}


dependencies {
    testImplementation("org.assertj:assertj-swing:3.17.1")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    implementation("commons-beanutils:commons-beanutils:1.9.4")
    implementation("com.google.apis:google-api-services-forms:v1-rev20220908-2.0.0")
    implementation("com.google.apis:google-api-services-drive:v3-rev197-1.25.0")
    implementation("com.google.api-client:google-api-client-jackson2:2.3.0")
    implementation("com.google.auth:google-auth-library-oauth2-http:1.23.0")

    implementation("org.tukaani:xz:1.9")
    implementation("org.apache.poi:poi-ooxml:5.2.5")
    implementation("org.asciidoctor:asciidoctorj-diagram:2.3.1")
    implementation("io.github.rburgst:okhttp-digest:3.1.1")
    implementation("org.ysb33r.gradle:grolifant:0.12.1")
    implementation("dev.langchain4j:langchain4j:$langchain4jVersion")
    implementation("commons-io:commons-io:$commonsIoVersion")

    // Jackson marshaller
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.fasterxml.jackson.module:jackson-module-jsonSchema:$jacksonVersion")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:$jacksonVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")

    // JGit
    implementation("org.eclipse.jgit:org.eclipse.jgit:$jgitVersion")
    implementation("org.eclipse.jgit:org.eclipse.jgit.archive:$jgitVersion")
    implementation("org.eclipse.jgit:org.eclipse.jgit.ssh.jsch:$jgitVersion")

    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:${properties["kotlinx-serialization-json.version"]}")

    // Spring tools
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    runtimeOnly("org.springframework.boot:spring-boot-properties-migrator")
    //developmentOnly("org.springframework.boot:spring-boot-docker-compose")

    // Springboot
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(module = "mockito-core")
    }
    // Spring security
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.security:spring-security-data")
    testImplementation("org.springframework.security:spring-security-test")

    // Spring cloud
    testImplementation("org.springframework.cloud:spring-cloud-starter-contract-verifier:${properties["spring_cloud_starter.version"]}") {
        exclude(module = "commons-collections")
    }

    // Spring tests
    // Spring-Shell
//    implementation("org.springframework.shell:spring-shell-starter")
//    testImplementation("org.springframework.shell:spring-shell-starter-test")

    // JWT
    implementation("io.jsonwebtoken:jjwt-impl:${properties["jsonwebtoken.version"]}")
    implementation("io.jsonwebtoken:jjwt-jackson:${properties["jsonwebtoken.version"]}")

    // SSL
    implementation("io.netty:netty-tcnative-boringssl-static:${properties["boring_ssl.version"]}")

    // Database
    runtimeOnly("org.postgresql:r2dbc-postgresql:${properties["r2dbc-postgresql.version"]}")

    // Kotlin-JUnit5
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")

    // Mock
    testImplementation("org.mockito.kotlin:mockito-kotlin:${properties["mockito_kotlin_version"]}")
    testImplementation("org.mockito:mockito-junit-jupiter:${properties["mockito_jupiter.version"]}")
    testImplementation("io.mockk:mockk:${properties["mockk.version"]}")
    testImplementation("org.wiremock:wiremock:${properties["wiremock.version"]}") {
        exclude(module = "commons-fileupload")
    }
    testImplementation("com.ninja-squad:springmockk:${properties["springmockk.version"]}")
    testImplementation("commons-fileupload:commons-fileupload:1.5.0.redhat-00001")

    // Archunit
    testImplementation("com.tngtech.archunit:archunit-junit5-api:${properties["archunit_junit5.version"]}")
    testRuntimeOnly("com.tngtech.archunit:archunit-junit5-engine:${properties["archunit_junit5.version"]}")

    // Langchain4j
    implementation("dev.langchain4j:langchain4j-spring-boot-starter:${properties["langchain4j.version"]}")
    implementation("dev.langchain4j:langchain4j-ollama-spring-boot-starter:${properties["langchain4j.version"]}")
    implementation("dev.langchain4j:langchain4j-hugging-face:${properties["langchain4j.version"]}")
    implementation("dev.langchain4j:langchain4j-google-ai-gemini:${properties["langchain4j.version"]}")
    implementation("dev.langchain4j:langchain4j-pgvector:${properties["langchain4j.version"]}")
//    implementation("dev.langchain4j:langchain4j-document-parser-apache-pdfbox:${properties["langchain4j.version"]}")
//    implementation("dev.langchain4j:langchain4j-web-search-engine-google-custom:${properties["langchain4j.version"]}")
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
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    testImplementation("io.projectreactor:reactor-test")

    // misc
    implementation("org.apache.commons:commons-lang3")
    testImplementation("org.apache.commons:commons-collections4:4.5.0-M1")
}

files("node_modules").run(idea.module.excludeDirs::plusAssign)

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
            }
        }
    }
}

tasks.withType<KotlinCompile> { kotlinOptions { freeCompilerArgs = listOf("-Xjsr305=strict") } }

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
    "app.CLI".run(mainClass::set)
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
            .toAbsolutePath()
    )
}