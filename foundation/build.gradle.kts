plugins {
    kotlin("multiplatform")
    id("maven-publish")
}

kotlin {
    jvm()
    js {
        browser()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("com.github.ajalt.colormath:colormath:2.0.0")
                api("org.jetbrains.kotlinx:kotlinx-datetime:0.1.0")
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
