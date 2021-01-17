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

        val jvmMain by getting {
            dependencies {
//                implementation("org.litote.kmongo:kmongo:4.2.3")
                implementation("org.litote.kmongo:kmongo-coroutine-serialization:4.2.3")
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
