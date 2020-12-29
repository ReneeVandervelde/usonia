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

    sourceSets {
        val commonMain by getting {
            resources.srcDirs(tasks.getByName("collectImportResources"))

            dependencies {
                api(project(":core"))
                api(project(":server"))
                api(project(":serialization"))
                dependencies.add("importResources", project(":web-frontend", "dist"))
                api(Kimchi.logger)
            }
        }
    }
}
