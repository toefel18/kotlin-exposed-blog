package nl.toefel.blog.exposed.db

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.jodatime.datetime

/**
 * Code representation of the movies table database DDL
 */
object Movies : Table("movies") {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 256)
    val producerName = varchar("producer_name", 255)
    val releaseDate = datetime("release_date")
    override val primaryKey = PrimaryKey(id)
}