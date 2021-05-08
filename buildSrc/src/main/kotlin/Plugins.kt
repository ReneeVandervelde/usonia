import org.gradle.plugin.use.PluginDependenciesSpec

fun PluginDependenciesSpec.multiplatformLibrary() = id("library.multiplatform")
fun PluginDependenciesSpec.backendlibrary() = id("library.backend")
