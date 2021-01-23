plugins {
    application
    kotlin("jvm")
    kotlin("kapt")
}

application {
    applicationName = "usonia"
    mainClassName = "usonia.cli.MainKt"
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

dependencies {
    implementation(project(":client-http"))
    implementation(project(":core"))
    implementation(project(":hubitat-bridge"))
    implementation(project(":hue-bridge"))
    implementation(project(":rules"))
    implementation(project(":schlage"))
    implementation(project(":serialization"))
    implementation(project(":server-ktor"))
    implementation(project(":smartthings"))
    implementation(project(":state-memory"))
    implementation(project(":state-sqldelight"))
    implementation(project(":todoist-bridge"))
    implementation(project(":weather"))
    implementation(project(":web-backend"))
    implementation(Coroutines.core)
    implementation(Kimchi.core)
    implementation("com.github.ajalt.clikt:clikt:3.0.1")
    implementation("com.google.dagger:dagger:2.30")
    kapt("com.google.dagger:dagger-compiler:2.30")
}
