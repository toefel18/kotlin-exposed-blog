package nl.toefel.blog.exposed.db

import org.jetbrains.exposed.sql.Table

/**
 * Code representation of the actors table database DDL
 */
object Actors : Table("actors") {
    val id = integer("id").autoIncrement().primaryKey()
    val firstName = varchar("first_name", 256)
    val lastName = varchar("last_name", 256)
    val dateOfBirth = date("date_of_birth").nullable()
}