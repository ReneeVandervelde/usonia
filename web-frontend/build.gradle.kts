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
    implementation(kotlinLibraries.coroutines.js)
    implementation(inkLibraries.kimchi.core)
    implementation(projects.kotlinExtensions)
    implementation(projects.clientHttp)
    implementation(projects.serialization)
    implementation(npm("mustache", "3.1.0"))
    implementation(npm("chart.js", "2.9.4"))
}
