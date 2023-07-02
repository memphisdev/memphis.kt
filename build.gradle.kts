import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    base
    kotlin("jvm") version "1.8.22"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.8.22"

    id("me.qoomon.git-versioning") version "6.4.2"

    `maven-publish`
}

group = "dev.memphis.sdk"
version = "0.0.0-SNAPSHOT"

gitVersioning.apply {
    refs {
        branch(".+") {
            version = "\${ref}-\${commit.short}-SNAPSHOT"
        }
        tag("v(?<version>.*)") {
            version = "\${ref.version}"
        }
    }

    // optional fallback configuration in case of no matching ref configuration
    rev {
        version = "\${commit}"
    }
}

allprojects {
    repositories {
        mavenCentral()
        maven("https://oss.sonatype.org/content/repositories/releases")
    }
}

dependencies {
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.7.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")

    implementation("io.nats:jnats:2.16.13")
    implementation("net.pwall.json:json-kotlin-schema:0.39")
    implementation("com.graphql-java:graphql-java:20.4")
    implementation("com.google.protobuf:protobuf-kotlin:3.23.3")

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
        register("dist", MavenPublication::class) {
            from(components["java"])
            artifact(sourcesJar.get())

            val artifactName = "${project.name}.kt"
            groupId = project.group.toString()
            artifactId = artifactName
            version = project.version.toString()

            pom {
                name.set("${project.group}:${artifactName}")
                description.set("Kotlin client for Memphis. Memphis is a Real-Time Data Processing Platform")
                url.set("https://memphis.dev")

                licenses {
                    license {
                        name.set("The Apache Software License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }

                scm {
                    url.set("https://github.com/memphisdev/memphis.kt")
                    connection.set("scm:git:https://github.com/memphisdev/memphis.kt.git")
                    developerConnection.set("scm:git:ssh://git@github.com/memphisdev/memphis.kt.git")
                }

            }
        }
    }
}
