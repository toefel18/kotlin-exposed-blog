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
        val h2ConnectionString = "jdbc:h2:mem:regular;DB_CLOSE_DELAY=-1;"

        @JvmStatic
        fun main(args: Array<String>) {
            logger.info("H2 database connection string: $h2ConnectionString")
            val db = Database.connect(h2ConnectionString, driver = "org.h2.Driver")
            db.useNestedTransactions = true // see https://github.com/JetBrains/Exposed/issues/605

            DatabaseInitializer.createSchemaAndTestData()

            Router(8080).start().printHints()
        }
    }
}





