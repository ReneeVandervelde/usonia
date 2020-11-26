plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization") version "1.4.10"
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
                api(KotlinX.dateTime)
                api(KotlinX.serialization)
                dependencies.add("importResources", project(":frontend", "dist"))
                api(Kimchi.logger)
            }
        }
    }
}
