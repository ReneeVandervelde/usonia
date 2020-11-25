package usonia.foundation

object FakeRooms {
    val LivingRoom = Room(
        id = Uuid("fake-living-room"),
        name = "Fake Living Room",
        type = Room.Type.LivingRoom,
    )
    val FakeHallway = Room(
        id = Uuid("fake-hallway"),
        name = "Fake Hallway",
    )
}
