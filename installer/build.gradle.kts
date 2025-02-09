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

dependencies.implementation(project.parent!!)

configurations.compileOnly { extendsFrom(configurations.annotationProcessor.get()) }

kotlin.compilerOptions
    .freeCompilerArgs
    .addAll("-Xjsr305=strict")

tasks {
    withType<JavaCompile>().configureEach { options.encoding = UTF_8.name() }
    withType<JavaExec>().configureEach { defaultCharacterEncoding = UTF_8.name() }
    withType<Javadoc>().configureEach { options.encoding = UTF_8.name() }
//    // Configuration du JAR
//    jar {
//        manifest {
//            attributes(
//                "Main-Class" to "app.workspace.Installer",
//                "Class-Path" to project.parent!!.configurations.runtimeClasspath.get().files.joinToString(" ") { it.name }
//            )
//        }
//        // Inclure toutes les dépendances dans le JAR
//        from(project.parent!!.configurations.compileClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
//        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
//    }
}