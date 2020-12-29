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
                implementation(project(":kotlin-extensions"))
                api(project(":foundation"))
            }
        }
    }
}
