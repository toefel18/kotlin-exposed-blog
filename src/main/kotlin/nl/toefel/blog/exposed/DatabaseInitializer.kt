package nl.toefel.blog.exposed

import nl.toefel.blog.exposed.db.Actors
import nl.toefel.blog.exposed.db.ActorsInMovies
import nl.toefel.blog.exposed.db.Movies
import nl.toefel.blog.exposed.dto.ActorDto
import nl.toefel.blog.exposed.dto.MovieWithActorDto
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Creates the schema and loads some test data
 */
object DatabaseInitializer {
    val logger: Logger = LoggerFactory.getLogger(DatabaseInitializer::class.java)

    fun createSchemaAndTestData() {
        logger.info("Creating/Updating schema")

        transaction {
            SchemaUtils.createMissingTablesAndColumns(Actors, Movies, ActorsInMovies)
        }

        val totalActors = transaction {
            Actors.selectAll().count()
        }

        if (totalActors > 0) {
            logger.info("There appears to be data already present, not inserting test data!")
            return
        }

        logger.info("Inserting actors and movies")

        val johnnyDepp = ActorDto(null, "Johnny", "Depp", "1979-10-28")
        val bradPitt = ActorDto(null, "Brad", "Pitt", "1982-05-16")
        val angelinaJolie = ActorDto(null, "Angelina", "Jolie", "1983-11-10")
        val jenniferAniston = ActorDto(null, "Jennifer", "Aniston", "175-07-23")
        val angelinaGrace = ActorDto(null, "Angelina", "Grace", "1988-09-02")
        val craigDaniel = ActorDto(null, "Craig", "Daniel", "1970-11-12")
        val ellenPaige = ActorDto(null, "Ellen", "Paige", "1981-12-20")
        val russellCrowe = ActorDto(null, "Russell", "Crowe", "1970-01-20")
        val edwardNorton = ActorDto(null, "Edward", "Norton", "1975-04-03")

        val actors = listOf(
            johnnyDepp,
            bradPitt,
            angelinaJolie,
            jenniferAniston,
            angelinaGrace,
            craigDaniel,
            ellenPaige,
            russellCrowe,
            edwardNorton
        )

        val movies = listOf(
            MovieWithActorDto(null, "Gladiator", "Universal Pictures", "2000-05-01", listOf(russellCrowe, ellenPaige, craigDaniel)),
            MovieWithActorDto(null, "Guardians of the galaxy", "Marvel", "2014-07-21", listOf(angelinaGrace, bradPitt, ellenPaige, angelinaJolie, johnnyDepp)),
            MovieWithActorDto(null, "Fight club", "FOX 2000", "1999-09-13", listOf(bradPitt, jenniferAniston, edwardNorton)),
            MovieWithActorDto(null, "13 Reasons Why", "Johnny", "2016-01-01", listOf(angelinaJolie, jenniferAniston))
        )

        transaction {
            SchemaUtils.create(Actors, Movies, ActorsInMovies)

            // batch insert items
            Actors.batchInsert(actors) {
                this[Actors.firstName] = it.firstName
                this[Actors.lastName] = it.lastName
                this[Actors.dateOfBirth] = DateTime.parse(it.dateOfBirth)
            }

            Movies.batchInsert(movies) {
                this[Movies.name] = it.name
                this[Movies.producerName] = it.producerName
                this[Movies.releaseDate] = DateTime.parse(it.releaseDate)
            }

            movies.forEach { movie ->
                val movieId = Movies
                    .slice(Movies.id)
                    .select { Movies.name eq movie.name }
                    .first()[Movies.id]

                movie.actors.forEach { actor ->
                    val actorId = Actors
                        .slice(Actors.id)
                        .select { (Actors.firstName eq actor.firstName) and (Actors.lastName eq actor.lastName) }
                        .first()[Actors.id]

                    ActorsInMovies.insert {
                        it[ActorsInMovies.actorId] = actorId
                        it[ActorsInMovies.movieId] = movieId
                    }
                }
            }
        }
    }
}