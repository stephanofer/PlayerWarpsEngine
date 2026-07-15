import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    java
    id("com.gradleup.shadow") version "8.3.11"
}

group = providers.gradleProperty("group").get()
version = providers.gradleProperty("version").get()
description = providers.gradleProperty("description").get()
val pluginVersion = version.toString()

repositories {
    mavenLocal()
    mavenCentral()

    maven {
        name = "placeholderapi"
        url = uri("https://repo.extendedclip.com/releases/")
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
    // The verified server and zMenu APIs are local and must never be shaded.
    compileOnly(files("libs/server.jar"))
    compileOnly(files("libs/zmenu-1.0.3.7.jar"))
    compileOnly("me.clip:placeholderapi:2.12.2")

    implementation("com.stephanofer.boostedyaml:boosted-yaml:1.3.7")
    implementation("com.github.ben-manes.caffeine:caffeine:2.9.3")
    implementation("com.zaxxer:HikariCP:4.0.3")
    implementation("com.mysql:mysql-connector-j:8.0.33")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(8)
}

tasks.processResources {
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand("version" to pluginVersion)
    }
}

tasks.named<Jar>("jar") {
    archiveClassifier.set("unshaded")
}

tasks.named<ShadowJar>("shadowJar") {
    archiveClassifier.set("")
    relocate("dev.dejvokep.boostedyaml", "com.hera.playerwarps.libs.boostedyaml")
    relocate("com.github.benmanes.caffeine", "com.hera.playerwarps.libs.caffeine")
    relocate("com.zaxxer.hikari", "com.hera.playerwarps.libs.hikari")
    mergeServiceFiles()
}

tasks.assemble {
    dependsOn(tasks.shadowJar)
}
