import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    application
    kotlin("jvm")
    kotlin("kapt")
}

application {
    applicationName = "usonia"
    mainClass.set("usonia.cli.MainKt")
}

sourceSets {
    main {
        resources {
            if (System.getProperty("idea.sync.active") == null) {
                srcDir(
                    "$rootDir"
                ).include("config.json")
            }
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_21
    }
}

dependencies {
    implementation(projects.clientHttp)
    implementation(projects.core)
    implementation(projects.serverTesting)
    implementation(projects.glassBridge)
    implementation(projects.hubitatBridge)
    implementation(projects.hueBridge)
    implementation(projects.rules)
    implementation(projects.serialization)
    implementation(projects.serverKtor)
    implementation(projects.stateMemory)
    implementation(projects.stateSqldelight)
    implementation(projects.telegramBridge)
    implementation(projects.weather)
    implementation(projects.webBackend)
    implementation(projects.notionBridge)
    implementation(libs.kotlin.coroutines.core)
    implementation(libs.kimchi.core)
    implementation("com.github.ajalt.clikt:clikt:5.0.3")
    implementation("org.slf4j:slf4j-nop:2.0.16")
}
