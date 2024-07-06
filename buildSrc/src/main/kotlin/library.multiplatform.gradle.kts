plugins {
    kotlin("multiplatform")
}

kotlin {
    jvmToolchain(15)
    jvm()
    js {
        browser()
    }
}
