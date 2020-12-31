plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm()

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":core-testing"))
                api(project(":server"))
                api(Coroutines.core)
            }
        }
    }
}
