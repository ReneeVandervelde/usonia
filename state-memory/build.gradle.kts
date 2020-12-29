plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm()

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":core"))
                api(project(":foundation"))
                api(Coroutines.core)
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation(Coroutines.test)
                implementation(project(":foundation-testing"))
                implementation(project(":core-testing"))
                implementation(kotlin("test-junit"))
                implementation(JUnit.core)
            }
        }
    }
}
