plugins {
    kotlin("js")
}

configurations {
    create("dist")
}

kotlin {
    js {
        browser()
    }
}

artifacts {
    add("dist", File("$buildDir/distributions/frontend-controls.js"))
    add("dist", File("$projectDir/src/main/html"))
}

dependencies {
    implementation(Coroutines.js)
    implementation(project(":client"))
}
