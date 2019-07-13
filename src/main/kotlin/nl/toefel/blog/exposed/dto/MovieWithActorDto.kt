package nl.toefel.blog.exposed.dto

data class MovieWithActorDto(
    val id: Int?,
    val name: String,
    val producerName: String,
    val releaseDate: String,
    val actors: List<ActorDto>
)