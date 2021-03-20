package usonia.foundation

object FakeUsers {
    val John = User(
        id = Identifier("fake-user-john"),
        name = "John Doe",
        parameters = emptyMap()
    )
    val Jane = User(
        id = Identifier("fake-user-jane"),
        name = "Jane Doe",
        parameters = emptyMap()
    )
}
