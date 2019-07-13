package nl.toefel.blog.exposed.db

import org.jetbrains.exposed.sql.Table

object Movies : Table("movies") {
    val id = integer("id").autoIncrement().primaryKey()
    val name = varchar("name", 256)
    val producerName = varchar("producer_name", 255)
    val releaseDate = datetime("release_date")
}