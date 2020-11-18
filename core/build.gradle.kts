plugins {
    kotlin("multiplatform")
}

configurations {
    create("importResources")
}

tasks {
    create("collectImportResources", Sync::class) {
        dependsOn(":frontend:assemble")
        into(temporaryDir).from(configurations.getByName("importResources"))
    }
}

kotlin {
    jvm()

    sourceSets {
        val commonMain by getting {
            resources.srcDirs(tasks.getByName("collectImportResources"))

            dependencies {
                implementation(project(":kotlin-extensions"))
                api(project(":server"))
                dependencies.add("importResources", project(":frontend", "dist"))
                api(Kimchi.logger)
            }
        }
    }
}
