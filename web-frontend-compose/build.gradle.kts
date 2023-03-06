plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose") version "1.3.0"
}

repositories {
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
}

configurations {
    create("dist")
}

artifacts {
    add("dist", File("$buildDir/distributions"))
}

kotlin {
    js(IR) {
        browser()
        binaries.executable()
    }
    sourceSets {
        val jsMain by getting {
            dependencies {
                implementation(compose.web.core)
                implementation(compose.runtime)
                implementation(kotlinLibraries.coroutines.js)
                implementation(inkLibraries.kimchi.core)
                implementation(projects.kotlinExtensions)
                implementation(projects.clientHttp)
                implementation(projects.serialization)
                implementation(npm("chart.js", "4.2.0"))
            }
        }
    }
}
