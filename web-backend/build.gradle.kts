plugins {
    kotlin("multiplatform")
}

configurations {
    create("importResources")
}

tasks {
    create("collectImportResources", Sync::class) {
        dependsOn(":web-frontend:assemble")
        into(temporaryDir).from(configurations.getByName("importResources"))
    }
}

kotlin {
    jvm()
    js {
        browser()
    }

    sourceSets {
        val commonMain by getting {
            resources.srcDirs(tasks.getByName("collectImportResources"))

            dependencies {
                api(project(":core"))
                api(project(":serialization"))
                dependencies.add("importResources", project(":web-frontend", "dist"))
                api(Kimchi.logger)
            }
        }
    }
}
