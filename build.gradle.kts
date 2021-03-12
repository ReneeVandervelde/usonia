import org.gradle.api.tasks.testing.logging.TestExceptionFormat

buildscript {
    repositories {
        jcenter()
        mavenCentral()
    }
    dependencies {
        classpath(kotlin("gradle-plugin", version = "1.4.30"))
        classpath("com.squareup.sqldelight:gradle-plugin:1.4.3")
    }
}

subprojects {
    repositories {
        jcenter()
        maven(url = "https://jitpack.io")
        maven(url = "https://kotlin.bintray.com/kotlinx/")
    }
    tasks.withType(Test::class) {
        testLogging.exceptionFormat = TestExceptionFormat.FULL
    }
}
