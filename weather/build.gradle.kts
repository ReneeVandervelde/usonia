plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization") version "1.4.10"
}

kotlin {
    jvm()

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":kotlin-extensions"))
                api(project(":core"))
                api(project(":foundation"))
                api(Coroutines.core)
                implementation(Ktor.client)
                implementation(Ktor.clientJson)
                implementation(Ktor.clientSerialization)
                implementation(KotlinX.dateTime)
                implementation(KotlinX.serialization)
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation(project(":core-testing"))
                implementation(kotlin("test-junit"))
                implementation(Coroutines.test)
            }
        }
    }
}
