plugins {
    backendlibrary()
}

configurations {
    create("importResources")
}

tasks {
    create("collectImportResources", Sync::class) {
        dependsOn(":${project.projects.webFrontend.name}:assemble")
        into(temporaryDir).from(configurations.getByName("importResources"))
    }
}

kotlin {
    sourceSets {
        val commonMain by getting {
            resources.srcDirs(tasks.getByName("collectImportResources"))

            dependencies {
                api(projects.core)
                api(projects.server)
                api(projects.serialization)
                dependencies.add("importResources", project(":${projects.webFrontend.name}", "dist"))
                api(inkLibraries.kimchi.logger)
            }
        }
    }
}
