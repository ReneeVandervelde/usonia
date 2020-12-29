/**
 * Group of dependencies that share group/version coordinates.
 */
abstract class DependencyGroup(
    val group: String,
    val version: String
) {
    fun dependency(
        name: String,
        group: String = this.group,
        version: String = this.version
    ) = "$group:$name:$version"
}

