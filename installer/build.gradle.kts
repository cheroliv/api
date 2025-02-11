import Build_gradle.Installer.CLASSPATH_KEY
import Build_gradle.Installer.GROUP_KEY
import Build_gradle.Installer.INSTALLER
import Build_gradle.Installer.KOTLIN_COMPILER_OPTION_JSR305
import Build_gradle.Installer.MAIN_CLASS_KEY
import Build_gradle.Installer.VERSION_KEY
import org.gradle.api.file.DuplicatesStrategy.EXCLUDE
import kotlin.text.Charsets.UTF_8

plugins {
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

object Installer {
    const val MAIN_CLASS_KEY = "Main-Class"
    const val CLASSPATH_KEY = "Class-Path"
    const val KOTLIN_COMPILER_OPTION_JSR305 = "-Xjsr305=strict"
    const val INSTALLER = "app.workspace.Installer"
    const val GROUP_KEY = "artifact.group"
    const val VERSION_KEY = "artifact.version"
}

group = properties[GROUP_KEY].toString()
version = properties[VERSION_KEY].toString()


dependencyManagement.imports {
    libs.versions.springboot.get()
        .run { "org.springframework.boot:spring-boot-dependencies:$this" }
        .run(::mavenBom)
}

parent?.let { dependencies.implementation(it) }

INSTALLER.run(application.mainClass::set)

configurations.compileOnly { extendsFrom(configurations.annotationProcessor.get()) }

kotlin.compilerOptions
    .freeCompilerArgs
    .addAll(KOTLIN_COMPILER_OPTION_JSR305)

tasks {
    withType<Jar> {
        dependsOn(parent?.tasks?.jar)
        manifest {
            attributes[MAIN_CLASS_KEY] = INSTALLER
            attributes[CLASSPATH_KEY] = configurations
                .runtimeClasspath.get()
                .joinToString(" ") { it.name }
        }
        duplicatesStrategy = EXCLUDE
        isZip64 = true
        from(parent?.sourceSets?.main?.get()?.output)
        from(
            configurations.runtimeClasspath.get().filter {
                it.name.endsWith("jar")
                        && !(it.name.contains("javadoc") ||
                        it.name.contains("plain") ||
                        it.name.contains("sources"))
            }.map(::zipTree)
        ) {
            // Exclude signed files
            exclude(
                "META-INF/*.SF",
                "META-INF/*.DSA",
                "META-INF/*.RSA",
                "META-INF/*.EC"
            )
        }
    }
    withType<JavaCompile>().configureEach { options.encoding = UTF_8.name() }
    withType<JavaExec>().configureEach { defaultCharacterEncoding = UTF_8.name() }
}