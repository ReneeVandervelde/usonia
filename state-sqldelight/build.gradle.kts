plugins {
    kotlin("multiplatform")
    id("com.squareup.sqldelight")
}

sqldelight {
    database("Database") {
        packageName = "usonia.state"
    }
}

kotlin {
    jvm()

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":core"))
                api(project(":foundation"))
                api(Coroutines.core)
                api(KotlinX.serialization)
            }
        }

        val jvmMain by getting {
            dependencies {
                implementation("com.squareup.sqldelight:sqlite-driver:1.4.3")
                implementation("com.squareup.sqldelight:coroutines-extensions:1.4.3")
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
