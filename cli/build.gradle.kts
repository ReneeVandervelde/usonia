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
    implementation(projects.clientHttp)
    implementation(projects.core)
    implementation(projects.hubitatBridge)
    implementation(projects.hueBridge)
    implementation(projects.rules)
    implementation(projects.serialization)
    implementation(projects.serverKtor)
    implementation(projects.stateMemory)
    implementation(projects.stateSqldelight)
    implementation(projects.todoistBridge)
    implementation(projects.telegramBridge)
    implementation(projects.weather)
    implementation(projects.webBackend)
    implementation(kotlinLibraries.coroutines.core)
    implementation(inkLibraries.kimchi.core)
    implementation("com.github.ajalt.clikt:clikt:3.5.0")
    implementation("com.google.dagger:dagger:2.44.2")
    kapt("com.google.dagger:dagger-compiler:2.44.2")
}
