plugins {
    backendlibrary()
}

tasks {
    val copyResources = create("copyResources", Sync::class) {
        dependsOn(":${project.projects.webFrontendCompose.name}:jsBrowserDistribution")
        from(project(":web-frontend-compose").file(project(":web-frontend-compose").layout.buildDirectory.dir("dist/js/productionExecutable")))
        into(layout.buildDirectory.dir("imported-resources"))
    }
    jvmProcessResources {
        if (this != copyResources) {
            dependsOn(copyResources)
        }
    }
}

kotlin {
    sourceSets {
        val commonMain by getting {
            resources.srcDirs(layout.buildDirectory.dir("imported-resources"))

            dependencies {
                api(projects.core)
                api(projects.server)
                api(projects.serialization)
                api(libs.kimchi.logger)
            }
        }
    }
}
