@file:Suppress("SpellCheckingInspection")

plugins {
    id("java")
    id("io.qameta.allure") version "2.12.0"
}

group = "ru.superkassa.tests"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

val allureVersion = "2.29.1"
val assertjVersion = "3.27.3"
val awaitilityVersion = "4.3.0"
val configVersion = "1.4.3"
val hikariVersion = "6.3.0"
val jacksonVersion = "2.19.1"
val junitVersion = "5.13.3"
val logbackVersion = "1.5.18"
val protobufVersion = "4.31.1"
val restAssuredVersion = "5.5.5"
val slf4jVersion = "2.0.17"
val testcontainersVersion = "1.21.3"

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-parameter-names:$jacksonVersion")
    implementation("com.google.protobuf:protobuf-java:$protobufVersion")
    implementation("com.typesafe:config:$configVersion")
    implementation("com.zaxxer:HikariCP:$hikariVersion")
    implementation("io.qameta.allure:allure-rest-assured:$allureVersion")
    implementation("io.rest-assured:rest-assured:$restAssuredVersion")
    implementation("org.awaitility:awaitility:$awaitilityVersion")
    implementation("org.slf4j:slf4j-api:$slf4jVersion")

    runtimeOnly("ch.qos.logback:logback-classic:$logbackVersion")

    testImplementation(platform("org.junit:junit-bom:$junitVersion"))
    testImplementation(platform("org.testcontainers:testcontainers-bom:$testcontainersVersion"))
    testImplementation("io.qameta.allure:allure-junit5:$allureVersion")
    testImplementation("org.assertj:assertj-core:$assertjVersion")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.postgresql:postgresql:42.7.7")
    testRuntimeOnly("org.junit.platform:junit-platform-suite")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
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
