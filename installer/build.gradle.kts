import org.gradle.api.file.DuplicatesStrategy.EXCLUDE
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
    application
    setOf(
        libs.plugins.kotlin.jvm to libs.versions.kotlin,
        libs.plugins.kotlin.allopen to libs.versions.kotlin,
        libs.plugins.kotlin.noarg to libs.versions.kotlin,
        libs.plugins.kotlin.serialization to libs.versions.kotlin,
        libs.plugins.versions to libs.versions.deps.versions,
        libs.plugins.spring.dependency.management to libs.versions.spring.dependency.management,
    ).forEach { id(it.first.get().pluginId).version(it.second) }
}

"app.workspace.Installer".run(application.mainClass::set)

dependencyManagement.imports {
    libs.versions.springboot.get()
        .run { "org.springframework.boot:spring-boot-dependencies:$this" }
        .run(::mavenBom)
}

parent?.let { dependencies.implementation(it) }

configurations.compileOnly { extendsFrom(configurations.annotationProcessor.get()) }

kotlin.compilerOptions
    .freeCompilerArgs
    .addAll("-Xjsr305=strict")

tasks {
    withType<JavaCompile>().configureEach { options.encoding = UTF_8.name() }
    withType<JavaExec>().configureEach { defaultCharacterEncoding = UTF_8.name() }
    withType<Javadoc>().configureEach { options.encoding = UTF_8.name() }
    withType<Jar> {
        dependsOn(parent?.tasks?.jar)
        manifest {
            attributes["Main-Class"] = "app.workspace.Installer"
            attributes["Class-Path"] = configurations
                .runtimeClasspath.get()
                .joinToString(" ") { it.name }
        }
        duplicatesStrategy = EXCLUDE
        isZip64 = true
        from(parent?.sourceSets?.main?.get()?.output)

        from(configurations.runtimeClasspath.get()
            .filter { it.name.endsWith("jar") }
            .map { zipTree(it) }
        ) {
            // Exclure les fichiers de signature
            exclude("META-INF/*.SF")
            exclude("META-INF/*.DSA")
            exclude("META-INF/*.RSA")
            exclude("META-INF/*.EC")
        }
    }
}