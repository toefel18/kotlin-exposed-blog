package nl.toefel.blog.exposed.db

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table

/**
 * Code representation of the bridge table database DDL to express a many-to-many relationship.
 */
object ActorsInMovies : Table("actors_in_movies") {
    val actorId = integer("actor_id").references(Actors.id, onDelete = ReferenceOption.CASCADE)
    val movieId = integer("movie_id").references(Movies.id, onDelete = ReferenceOption.CASCADE)
    override val primaryKey = PrimaryKey(actorId, movieId)
}