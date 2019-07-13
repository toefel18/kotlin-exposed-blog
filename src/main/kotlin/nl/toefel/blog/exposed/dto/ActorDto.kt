package nl.toefel.blog.exposed.dto

data class ActorDto(
    val id: Int?,
    val firstName: String,
    val lastName: String,
    val dateOfBirth: String?
)