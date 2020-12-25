package usonia.foundation

object FakeRooms {
    val LivingRoom = Room(
        id = Identifier("fake-living-room"),
        name = "Fake Living Room",
        type = Room.Type.LivingRoom,
    )
    val FakeHallway = Room(
        id = Identifier("fake-hallway"),
        name = "Fake Hallway",
    )
}
