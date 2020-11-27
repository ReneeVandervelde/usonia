plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm()

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":kotlin-extensions"))
                api(project(":core"))
                api(project(":state"))
                api(project(":serialization"))
                api(project(":foundation"))
                api(Coroutines.core)
                implementation(Ktor.client)
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-junit"))
                implementation(Coroutines.test)
                implementation(project(":state-testing"))
                implementation(project(":foundation-testing"))
            }
        }
    }
}
