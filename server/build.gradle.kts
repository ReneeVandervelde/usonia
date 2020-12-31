plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":kotlin-extensions"))
                implementation(project(":foundation"))
                implementation(project(":core"))
                api(KotlinX.dateTime)
                api(KotlinX.serialization)
                api(Kimchi.logger)
            }
        }
    }
}
