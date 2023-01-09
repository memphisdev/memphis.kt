import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    base
    kotlin("jvm") version "1.7.22"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.7.22"

    `maven-publish`
}

group = "dev.memphis.sdk"
version = "1.0-SNAPSHOT"

allprojects {
    repositories {
        mavenCentral()
        maven("https://oss.sonatype.org/content/repositories/releases")
    }
}

dependencies {
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.4")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.6.4")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")

    implementation("io.nats:jnats:2.16.5")
    implementation("net.pwall.json:json-kotlin-schema:0.39")
    implementation("com.graphql-java:graphql-java:20.0")
    implementation("com.google.protobuf:protobuf-kotlin:3.21.9")

    testImplementation(kotlin("test"))

}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

subprojects {
    apply {
        plugin("kotlin")
    }
    val compileKotlin: KotlinCompile by tasks
    compileKotlin.kotlinOptions {
        jvmTarget = "1.8"
    }

    val compileTestKotlin: KotlinCompile by tasks
    compileTestKotlin.kotlinOptions {
        jvmTarget = "1.8"
    }

    dependencies {
        implementation("ch.qos.logback:logback-classic:1.4.5")
        implementation("net.logstash.logback:logstash-logback-encoder:7.1.1")

        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.6.4")
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")
        implementation("io.nats:jnats:2.16.5")
        implementation("net.pwall.json:json-kotlin-schema:0.39")

        implementation("com.google.protobuf:protobuf-kotlin:3.21.9")

        testImplementation(kotlin("test"))
        implementation(project(mapOf("path" to ":")))
    }
}


project(":examples") {
    rootProject
}

val sourcesJar by tasks.registering(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets["main"].allSource)
}

publishing {
    publications {
        register("mavenJava", MavenPublication::class) {
            from(components["java"])
            artifact(sourcesJar.get())
            groupId = project.group.toString()
            artifactId = project.name
            version = project.version.toString()
        }
    }
}
