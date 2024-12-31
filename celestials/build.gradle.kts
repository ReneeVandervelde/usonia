plugins {
    backendlibrary()
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.server)
            api(libs.kotlin.coroutines.core)
            api(libs.kotlin.datetime)
        }

        jvmMain.dependencies {
            implementation(libs.sunrisesunsetcalculator)
        }

        commonTest.dependencies {
            implementation(projects.coreTesting)
            implementation(projects.serverTesting)
            implementation(libs.kotlin.test.core)
            implementation(libs.kotlin.coroutines.test)
        }
    }
}
