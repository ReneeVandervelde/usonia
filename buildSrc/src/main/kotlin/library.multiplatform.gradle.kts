plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm {
        jvmToolchain(15)
    }
    js(BOTH) {
        browser()
    }
}
