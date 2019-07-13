package nl.toefel.blog.exposed

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import nl.toefel.blog.exposed.rest.Router
import org.jetbrains.exposed.sql.Database
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.math.min

/**
 * Connects to a Postgres database using a HikariCP connection pool and configures the schema and test data,
 * then starts a REST API at 8080.
 *
 * Using docker, you can start a local Postgres database as follows:
 * ```
 * docker run --name exposed-db -p 5432:5432 -e POSTGRES_USER=exposed -e POSTGRES_PASSWORD=exposed -d postgres
 * ```
 *
 * If postgres is not running, it will retry connecting with a back-off
 */
class MainWithPostgresAndHikari {
    companion object {
        private val logger: Logger = LoggerFactory.getLogger(MainWithPostgresAndHikari::class.java)

        @JvmStatic
        fun main(args: Array<String>) {
            logger.info("Creating a HikariCP data source")
            val hikariDataSource = createHikariDataSourceWithRetry(
                jdbcUrl = "jdbc:postgresql://127.0.0.1:5432/exposed",
                username = "exposed",
                password = "exposed")

            // Database.connect will configure the HikariDataSource for use by Kotlin Exposed.
            // Code can start a transaction to interact with the database
            //
            // transaction {
            //     Interact with tables like Actors/Movies
            // }
            Database.connect(hikariDataSource)

            DatabaseInitializer.createSchemaAndTestData()

            Router(8080).start().printHints()
        }

        const val maxBackoffMillis = 16000L
        val defaultBackoffMillis = generateSequence(1000L) { min(it * 2, maxBackoffMillis) }

        /**
         * Creates a HikariDataSource and returns it. If any exception is thrown, the operation is retried after x millis as
         * defined in the backoff sequence. If the sequence runs out of entries, the operation fails with the last
         * encountered exception.
         */
        tailrec fun createHikariDataSourceWithRetry(jdbcUrl: String, username: String, password: String,
                                                    backoffSequence: Iterator<Long> = defaultBackoffMillis.iterator()): HikariDataSource {
            try {
                val config = HikariConfig()
                config.jdbcUrl = jdbcUrl
                config.username = username
                config.password = password
                config.driverClassName = "org.postgresql.Driver"
                return HikariDataSource(config)
            } catch (ex: Exception) {
                logger.error("Failed to create data source ${ex.message}")
                if (!backoffSequence.hasNext()) throw ex
            }
            val backoffMillis = backoffSequence.next()
            logger.info("Trying again in $backoffMillis millis")
            Thread.sleep(backoffMillis)
            return createHikariDataSourceWithRetry(jdbcUrl, username, password, backoffSequence)
        }
    }
}





