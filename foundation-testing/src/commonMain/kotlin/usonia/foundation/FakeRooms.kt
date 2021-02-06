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
        type = Room.Type.Hallway,
    )
    val FakeBathroom = Room(
        id = Identifier("fake-bathroom"),
        name = "Fake Bathroom",
        type = Room.Type.Bathroom,
    )
    val FakeBedroom = Room(
        id = Identifier("fake-bedroom"),
        name = "Fake Bedroom",
        type = Room.Type.Bedroom,
    )
}
