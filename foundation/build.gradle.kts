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
                api("com.github.ajalt.colormath:colormath:2.0.0")
                api(project(":kotlin-extensions"))
                api(KotlinX.dateTime)
                api(project(":kotlin-extensions"))
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }

        val jvmMain by getting {
            dependencies {}
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-junit"))
                implementation(JUnit.core)
            }
        }
        val jsMain by getting {
            dependencies {
                implementation(npm("uuid", "8.3.1"))
            }
        }
    }
}
