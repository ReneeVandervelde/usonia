plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm() {

        java {
            sourceCompatibility = org.gradle.api.JavaVersion.VERSION_15
            targetCompatibility = org.gradle.api.JavaVersion.VERSION_15
        }
    }
    js(BOTH) {
        browser()
    }
}
