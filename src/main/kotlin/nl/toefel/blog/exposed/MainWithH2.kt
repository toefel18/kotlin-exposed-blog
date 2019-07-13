package nl.toefel.blog.exposed

import nl.toefel.blog.exposed.rest.Router
import org.jetbrains.exposed.sql.Database
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Starts an in-memory H2 database, creates the schema and loads some test data
 */
class MainWithH2 {
    companion object {
        private val logger: Logger = LoggerFactory.getLogger(MainWithH2::class.java)

        @JvmStatic
        fun main(args: Array<String>) {
            val h2ConnectionString = "jdbc:h2:mem:regular;DB_CLOSE_DELAY=-1;"

            logger.info("H2 database connection string: $h2ConnectionString")

            // Database.connect will initialize and track H2 for use by Kotlin Exposed.
            // Code can start a transaction to interact with the database
            //
            // transaction {
            //     Interact with tables like Actors/Movies
            // }
            Database.connect(h2ConnectionString, driver = "org.h2.Driver")

            DatabaseInitializer.createSchemaAndTestData()

            Router(8080).start().printHints()
        }
    }
}





