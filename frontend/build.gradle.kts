plugins {
    kotlin("js")
}

configurations {
    create("dist")
}

kotlin {
    js {
        browser()
        useCommonJs()
    }
}

artifacts {
    add("dist", File("$buildDir/distributions"))
    add("dist", File("$projectDir/src/main/html"))
}

dependencies {
    implementation(Coroutines.js)
    implementation(Kimchi.core)
    implementation(project(":kotlin-extensions"))
    implementation(project(":client"))
    implementation(npm("mustache", "3.1.0"))
}
