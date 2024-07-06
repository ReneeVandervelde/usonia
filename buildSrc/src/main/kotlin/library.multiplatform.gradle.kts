plugins {
    kotlin("multiplatform")
}

kotlin {
    jvmToolchain(17)
    jvm()
    js {
        browser()
    }
}
