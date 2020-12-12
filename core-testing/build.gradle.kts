plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm()
    js {
        browser()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":foundation-testing"))
                api(project(":core"))
                api(Coroutines.core)
            }
        }
    }
}
