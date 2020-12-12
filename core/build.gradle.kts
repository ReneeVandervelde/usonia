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
                implementation(project(":serialization"))
                api(KotlinX.dateTime)
                api(Kimchi.logger)
            }
        }
    }
}
