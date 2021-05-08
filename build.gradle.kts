import org.gradle.api.tasks.testing.logging.TestExceptionFormat

subprojects {
    repositories {
        mavenCentral()
        maven(url = "https://kotlin.bintray.com/kotlinx/")
        maven(url = "https://jitpack.io")
    }
    tasks.withType(Test::class) {
        testLogging.exceptionFormat = TestExceptionFormat.FULL
    }
}
