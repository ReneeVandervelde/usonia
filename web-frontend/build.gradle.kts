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
    add("dist", File("$projectDir/src/main/css"))
}

dependencies {
    implementation(Coroutines.js)
    implementation(Kimchi.core)
    implementation(project(":kotlin-extensions"))
    implementation(project(":client"))
    implementation(project(":smartthings"))
    implementation(project(":hue-bridge"))
    implementation(project(":schlage"))
    implementation(npm("mustache", "3.1.0"))
}
