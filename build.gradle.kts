plugins {
    id("java")
    id("com.diffplug.spotless") version "8.1.0"
    id("checkstyle")
    id("pmd")
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.assertj:assertj-core:3.27.7")
    testImplementation("org.junit.jupiter:junit-jupiter:6.0.3")
    testImplementation("org.mockito:mockito-core:5.12.0")
    testImplementation(platform("org.junit:junit-bom:5.11.0"))
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:6.0.3")
}

tasks.test {
    useJUnitPlatform()
}

// to enable virtual threads: START
tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("--enable-preview")
}

tasks.withType<JavaExec>().configureEach {
    jvmArgs("--enable-preview")
}

tasks.withType<Test>().configureEach {
    jvmArgs("--enable-preview")
}
// to enable virtual threads: END

spotless { // pre-commit
    java {
        target("src/**/*.java")
        targetExclude("**/build/**")
        googleJavaFormat("1.33.0")
        trimTrailingWhitespace()
        endWithNewline()
        removeUnusedImports()
    }
}

checkstyle { // pre-commit
    toolVersion = "12.1.2"
    configFile = file("config/checkstyle/checkstyle.xml")
    configProperties = mapOf(
        "org.checkstyle.google.suppressionfilter.config" to
                file("config/checkstyle/checkstyle-suppressions.xml").absolutePath
    )
}

pmd { // pre-commit
    toolVersion = "6.55.0"
}
