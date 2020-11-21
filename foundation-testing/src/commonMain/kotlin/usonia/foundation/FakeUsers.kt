package usonia.foundation

object FakeUsers {
    val John = User(
        id = Uuid("fake-user-john"),
        name = "John Doe",
        parameters = emptyMap()
    )
}
