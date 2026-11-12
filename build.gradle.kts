plugins {
    java
}

// release instead of a toolchain: any JDK 17+ builds it, no exact JDK 17 has to be installed.
tasks.withType<JavaCompile>().configureEach {
    options.release = 17
    options.encoding = "UTF-8"
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("io.cucumber:cucumber-bom:7.20.1"))
    testImplementation(platform("org.junit:junit-bom:5.11.3"))

    testImplementation("io.cucumber:cucumber-java")
    testImplementation("io.cucumber:cucumber-junit-platform-engine")
    testImplementation("io.cucumber:cucumber-picocontainer")
    testImplementation("org.junit.platform:junit-platform-suite")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.wiremock:wiremock-standalone:3.10.0")
    testImplementation("com.fasterxml.jackson.core:jackson-databind:2.18.2")
    testImplementation("com.jayway.jsonpath:json-path:2.9.0")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    // json-path logs through SLF4J; without a provider every run prints a warning.
    testRuntimeOnly("org.slf4j:slf4j-nop:2.0.16")
}

tasks.test {
    useJUnitPlatform()
    // Gradle does not pass -D flags to the test JVM; forward the ones the suite reads.
    System.getProperties()
        .filterKeys { it.toString().startsWith("cucumber.") || it.toString().startsWith("edpn.") }
        .forEach { (key, value) -> systemProperty(key.toString(), value) }
    testLogging {
        events("passed", "failed", "skipped")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}
