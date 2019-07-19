package nl.toefel.blog.exposed.rest

import io.javalin.Javalin
import io.javalin.http.Context
import nl.toefel.blog.exposed.db.Actors
import nl.toefel.blog.exposed.db.ActorsInMovies
import nl.toefel.blog.exposed.db.Movies
import nl.toefel.blog.exposed.dto.ActorDto
import nl.toefel.blog.exposed.dto.MovieActorCountDto
import nl.toefel.blog.exposed.dto.MovieSummary
import nl.toefel.blog.exposed.dto.MovieWithActorDto
import nl.toefel.blog.exposed.dto.MovieWithProducingActorDto
import org.jetbrains.exposed.sql.Join
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Creates the webserver:
 * 1. configures a request logger
 * 2. enables CORS on all domains
 * 3. configures available paths and their handlers
 * 4. transforms database results to and from DTOs (client interface)
 */
class Router(val port: Int) {
    private val logger: Logger = LoggerFactory.getLogger(Router::class.java)

    val app = Javalin.create { cfg -> cfg.requestLogger(::logRequest).enableCorsForAllOrigins() }
        .get("/actors", ::listAndFilterActors)
        .post("/actors", ::createActor)
        .delete("/actors/:id", ::deleteActor)
        .get("/movies", ::listMovies)
        .get("/movies/:id", ::getMovie)
        .get("/moviesActorCount", ::listMovieActorCount)
        .get("/moviesWithActingProducers", ::listMoviesWithActingProducers)

    private fun logRequest(ctx: Context, executionTimeMs: Float) =
        logger.info("${ctx.method()} ${ctx.fullUrl()} status=${ctx.status()} durationMs=$executionTimeMs")

    fun start(): Router {
        app.start(port)
        return this
    }

    fun printHints() {
        logger.info("Navigate to: http://localhost:8080/actors")
        logger.info("Navigate to: http://localhost:8080/actors?firstName=Angelina")
        logger.info("Navigate to: http://localhost:8080/movies")
        logger.info("Navigate to: http://localhost:8080/movies/2")
        logger.info("Navigate to: http://localhost:8080/moviesActorCount")
        logger.info("Navigate to: http://localhost:8080/moviesWithActingProducers")
    }

    fun listAndFilterActors(ctx: Context) {
        val actorDtos = transaction {
            // uses the connections initialized via Database.connect() in main!
            val actorsQuery = Actors.selectAll()

            // enable additional filters via query params if they are present (example: ?firstName=Angelina)
            ctx.queryParam("id")?.let { actorsQuery.andWhere { Actors.id eq it.toInt() } }
            ctx.queryParam("firstName")?.let { actorsQuery.andWhere { Actors.firstName eq it } }
            ctx.queryParam("lastName")?.let { actorsQuery.andWhere { Actors.lastName eq it } }
            ctx.queryParam("dateOfBirth")?.let { actorsQuery.andWhere { Actors.dateOfBirth eq DateTime.parse(it) } }

            // map the database result rows to a DTO for the client
            actorsQuery.map { mapToActorDto(it) }
        }
        ctx.json(actorDtos)
    }

    fun createActor(ctx: Context) {
        val actorDto = ctx.bodyAsClass(ActorDto::class.java)
        val insertedActorId = transaction {
            Actors.insert {
                it[firstName] = actorDto.firstName
                it[lastName] = actorDto.lastName
                it[dateOfBirth] = if (actorDto.dateOfBirth != null) DateTime.parse(actorDto.dateOfBirth) else null
            } get Actors.id // fetches the auto generated ID
        }

        ctx.json(actorDto.copy(id = insertedActorId))
    }

    fun deleteActor(ctx: Context) {
        val actorId = ctx.pathParam("id").toIntOrNull()
        if (actorId == null) {
            ctx.json("invalid id").status(400)
        } else {
            val deletedCount = transaction { Actors.deleteWhere { Actors.id eq actorId } }

            if (deletedCount == 0) {
                ctx.json("no actor found with id $actorId").status(404)
            } else {
                ctx.json("actor with id $actorId deleted").status(200)
            }
        }
    }

    fun listMovies(ctx: Context) {
        val allMoviesDtos = transaction {
            val moviesQuery = Movies.selectAll()

            // optional: enable additional filters via query params (example: ?name=Gladiator)
            ctx.queryParam("id")?.let { moviesQuery.andWhere { Movies.id eq it.toInt() } }
            ctx.queryParam("name")?.let { moviesQuery.andWhere { Movies.name eq it } }
            ctx.queryParam("producerName")?.let { moviesQuery.andWhere { Movies.producerName eq it } }
            ctx.queryParam("releaseDate")?.let { moviesQuery.andWhere { Movies.releaseDate eq DateTime.parse(it) } }

            // map the result
            moviesQuery.map { mapToMovieSummaryDto(it) }
        }
        ctx.json(allMoviesDtos)
    }

    fun getMovie(ctx: Context) {
        val movieId = ctx.pathParam("id").toIntOrNull()
        if (movieId == null) {
            ctx.json("invalid id").status(400)
        } else {
            val movieDto: MovieWithActorDto? = transaction {
                val movieOrNull = Movies.select { Movies.id eq movieId }.firstOrNull()

                // if movie is not null, fetch the actors and map to the DTO
                movieOrNull?.let { movie ->
                    val actors = ActorsInMovies
                        .innerJoin(Actors)
                        .innerJoin(Movies)
                        .slice(Actors.columns) // only select these columns to reduce data load
                        .select { Movies.id eq movieId }
                        .map { mapToActorDto(it) }

                    mapToMovieWithActorDto(movie, actors)
                }
            }

            if (movieDto == null) {
                ctx.json("no movie found with id $movieId").status(404)
            } else {
                ctx.json(movieDto).status(200)
            }
        }
    }

    fun listMovieActorCount(ctx: Context) {
        val movieActorCounts = transaction {
            Movies
                .innerJoin(ActorsInMovies)
                .innerJoin(Actors)
                .slice(Movies.name, Actors.firstName.count())
                .selectAll()
                .groupBy(Movies.name)
                .map { MovieActorCountDto(it[Movies.name], it[Actors.firstName.count()]) }
        }
        ctx.json(movieActorCounts).status(200)
    }

    /**
     * Lists all movies that have a producer which is also known as an actor
     */
    fun listMoviesWithActingProducers(ctx: Context) {
        val moviesProducedByActors = transaction {
            Join(Actors, Movies, JoinType.INNER, additionalConstraint = { Actors.firstName eq Movies.producerName })
                .slice(Movies.name, Actors.firstName, Actors.lastName)
                .selectAll()
                .map { MovieWithProducingActorDto(it[Movies.name], "${it[Actors.firstName]} ${it[Actors.lastName]}") }
        }
        ctx.json(moviesProducedByActors).status(200)
    }
}


fun mapToActorDto(it: ResultRow) = ActorDto(
    id = it[Actors.id],
    firstName = it[Actors.firstName],
    lastName = it[Actors.lastName],
    dateOfBirth = it[Actors.dateOfBirth]?.toString("yyyy-MM-dd"))

fun mapToMovieWithActorDto(it: ResultRow, actors: List<ActorDto>) = MovieWithActorDto(
    id = it[Movies.id],
    name = it[Movies.name],
    producerName = it[Movies.producerName],
    releaseDate = it[Movies.releaseDate].toString("yyyy-MM-dd"),
    actors = actors)

fun mapToMovieSummaryDto(it: ResultRow) = MovieSummary(
    id = it[Movies.id],
    name = it[Movies.name],
    producerName = it[Movies.producerName],
    releaseDate = it[Movies.releaseDate].toString("yyyy-MM-dd"))