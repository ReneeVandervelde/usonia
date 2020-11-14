plugins {
    kotlin("jvm")
}
sourceSets {
    main {
        resources {
            srcDir( "${projectDir.path}/src/main/html")
        }
    }
}

dependencies {
    api(project(":server"))
    implementation(project(":kotlin-extensions"))
    api(Kimchi.logger)
}
