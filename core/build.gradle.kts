plugins {
    kotlin("jvm")
}

configurations {
    create("importResources")
}

tasks {
    create("collectImportResources", Sync::class) {
        into(temporaryDir).from(configurations.getByName("importResources"))
    }
    compileJava {
        dependsOn(":frontend:assemble")
    }
}

sourceSets {
    main {
        resources {
            srcDirs(
                tasks.getByName("collectImportResources")
            )
        }
    }
}

dependencies {
    api(project(":server"))
    implementation(project(":kotlin-extensions"))
    "importResources"(project(":frontend", "dist"))
    api(Kimchi.logger)
}
