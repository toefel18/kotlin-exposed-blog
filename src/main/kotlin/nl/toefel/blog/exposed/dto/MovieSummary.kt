package nl.toefel.blog.exposed.dto

data class MovieSummary(
    val id: Int?,
    val name: String,
    val producerName: String,
    val releaseDate: String
)