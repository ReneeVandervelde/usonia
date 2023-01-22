plugins {
    backendlibrary()
}

configurations {
    create("importResources")
}

tasks {
    create("collectDistributions", Sync::class) {
        dependsOn(":${project.projects.webFrontendCompose.name}:jsBrowserDistribution")
        into(temporaryDir).from(configurations.getByName("importResources"))
    }
}

kotlin {
    sourceSets {
        val commonMain by getting {
            resources.srcDirs(tasks.getByName("collectDistributions"))

            dependencies {
                api(projects.core)
                api(projects.server)
                api(projects.serialization)
                dependencies.add("importResources", project(":${projects.webFrontendCompose.name}", "dist"))
                api(inkLibraries.kimchi.logger)
            }
        }
    }
}
