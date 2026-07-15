plugins {
    id("java")
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.allure)
}

group = "kz.superkassa.tests"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

val allureReportVersion = libs.versions.allure.report.get()
val allureJavaAdapterVersion = libs.versions.allure.java.get()

allure {
    version.set(allureReportVersion)
    report {
        configFile.set(layout.projectDirectory.file("config/allurerc.json"))
        singleFile.set(true)
    }
    adapter {
        allureJavaVersion.set(allureJavaAdapterVersion)
        frameworks {
            junit5 {
                enabled.set(true)
            }
        }
    }
}

tasks.named("installAllure3") {
    doFirst {
        if (!System.getProperty("os.name").lowercase().contains("windows")) {
            val nodeBin = layout.buildDirectory.file("allure/node/bin/node").get().asFile
            val npmBin = layout.buildDirectory.file("allure/node/bin/npm").get().asFile
            val npmCli = layout.buildDirectory.file("allure/node/lib/node_modules/npm/bin/npm-cli.js").get().asFile

            npmBin.delete()
            npmBin.writeText(
                """
                #!/bin/sh
                exec "${nodeBin.absolutePath}" "${npmCli.absolutePath}" "$@"
                """.trimIndent()
            )
            npmBin.setExecutable(true)
        }
    }
}

tasks.named<io.qameta.allure.gradle.report.tasks.AllureReport>("allureReport") {
    resultsDirs.setFrom(layout.buildDirectory.dir("allure-results"))
    mustRunAfter(tasks.withType<Test>())
}

tasks.named<io.qameta.allure.gradle.report.tasks.AllureServe>("allureServe") {
    resultsDirs.setFrom(layout.buildDirectory.dir("allure-results"))
    mustRunAfter(tasks.withType<Test>())
    actions.clear()
    doLast {
        val allureExecutable = if (System.getProperty("os.name").lowercase().contains("windows")) {
            layout.buildDirectory.file("allure/commandline/bin/allure.bat").get().asFile
        } else {
            layout.buildDirectory.file("allure/commandline/bin/allure").get().asFile
        }
        val resultsDir = layout.buildDirectory.dir("allure-results").get().asFile
        val reportDir = layout.buildDirectory.dir("reports/allure-report/allureServe").get().asFile
        val config = layout.projectDirectory.file("config/allurerc.json").asFile

        val command = mutableListOf(
            allureExecutable.absolutePath,
            "generate",
            resultsDir.absolutePath,
            "--config",
            config.absolutePath,
            "--output",
            reportDir.absolutePath,
            "--open"
        )
        port.orNull?.let {
            command.addAll(listOf("--port", it.toString()))
        }

        val process = ProcessBuilder(command)
            .directory(projectDir)
            .inheritIO()
            .start()
        val exitCode = process.waitFor()
        if (exitCode != 0) {
            throw GradleException(
                "Allure serve failed with exit code $exitCode. Command: ${
                    command.joinToString(" ")
                }"
            )
        }
    }
}

dependencies {
    implementation(libs.jackson.databind)
    implementation(libs.protobuf.java)
    implementation(libs.config)
    implementation(libs.hikari)
    implementation(libs.allure.rest.assured)
    implementation(libs.rest.assured)
    implementation(libs.awaitility)
    implementation(libs.commons.codec)
    implementation(libs.commons.lang3)
    implementation(libs.slf4j.api)

    runtimeOnly(libs.logback.classic)

    constraints {
        implementation(libs.commons.compress)
    }

    testImplementation(platform(libs.junit.bom))
    testImplementation(platform(libs.testcontainers.bom))
    testImplementation(libs.allure.junit5)
    testImplementation(libs.assertj.core)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.jupiter.params)
    testImplementation(libs.testcontainers.junit)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.postgresql)
    testRuntimeOnly(libs.junit.platform.suite)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    systemProperty("file.encoding", "UTF-8")
    systemProperty("allure.results.directory", layout.buildDirectory.dir("allure-results").get().asFile)
    testLogging {
        events("failed", "skipped")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

fun registerLayerTask(name: String, tagExpression: String) {
    tasks.register<Test>(name) {
        group = "verification"
        description = "Runs tests matching tag expression: $tagExpression."
        testClassesDirs = sourceSets.test.get().output.classesDirs
        classpath = sourceSets.test.get().runtimeClasspath
        useJUnitPlatform {
            includeTags(tagExpression)
        }
        shouldRunAfter(tasks.test)
    }
}

registerLayerTask("apiSmokeTest", "api & smoke")
registerLayerTask("apiRegressionTest", "api & regression")
registerLayerTask("functionalSmokeTest", "functional & smoke")
registerLayerTask("functionalRegressionTest", "functional & regression")
registerLayerTask("protocolSmokeTest", "protocol & smoke")
registerLayerTask("protocolRegressionTest", "protocol & regression")
registerLayerTask("dbSmokeTest", "db & smoke")
registerLayerTask("dbRegressionTest", "db & regression")

tasks.register("smokeTest") {
    group = "verification"
    description = "Runs all smoke suites."
    dependsOn("apiSmokeTest", "functionalSmokeTest", "protocolSmokeTest", "dbSmokeTest")
}

tasks.register("regressionTest") {
    group = "verification"
    description = "Runs all regression suites."
    dependsOn("apiRegressionTest", "functionalRegressionTest", "protocolRegressionTest", "dbRegressionTest")
}
