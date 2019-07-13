package nl.toefel.blog.exposed.db

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table

object ActorsInMovies : Table("actors_in_movies") {
    val actorId = integer("actor_id").references(Actors.id, onDelete = ReferenceOption.CASCADE).primaryKey()
    val movieId = integer("movie_id").references(Movies.id, onDelete = ReferenceOption.CASCADE).primaryKey()
}