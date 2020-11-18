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
    add("dist", File("$buildDir/distributions/frontend.js"))
    add("dist", File("$projectDir/src/main/html"))
}

dependencies {
    implementation(Coroutines.js)
    implementation(project(":client"))
}
