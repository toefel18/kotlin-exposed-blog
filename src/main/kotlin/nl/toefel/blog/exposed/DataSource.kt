package nl.toefel.blog.exposed

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.slf4j.Logger
import org.slf4j.LoggerFactory


const val maxBackoffMs = 16000L
val defaultBackoffMsSequence = generateSequence(1000L) { Math.min(it * 2, maxBackoffMs) }
val logger: Logger = LoggerFactory.getLogger("DataSource.kt")


/**
 * Creates a HikariDataSource and returns it. If any exception is thrown, the operation is retried after x millis as
 * defined in the backoff sequence. If the sequence runs out of entries, the operation fails with the last
 * encountered exception.
 *
 * Start a PostgreSQL container with:
 * ```
 * docker run --name exposed-db -p 5432:5432 -e POSTGRES_USER=exposed -e POSTGRES_PASSWORD=exposed -d postgres
 * ```
 */
tailrec fun createHikariDataSource(backoffSequence: Iterator<Long> = defaultBackoffMsSequence.iterator()): HikariDataSource {
    try {
        val config = HikariConfig()
        config.jdbcUrl = "jdbc:postgresql://127.0.0.1/exposed" // defaults to port 5432
        config.username = "exposed"
        config.password = "exposed"
        config.driverClassName = "org.postgresql.Driver"
        return HikariDataSource(config)
    } catch (ex: Exception) {
        logger.error("Failed to create data source ${ex.message}")
        if (!backoffSequence.hasNext()) throw ex
    }
    val backoffMillis = backoffSequence.next()
    logger.info("Trying again in $backoffMillis millis")
    Thread.sleep(backoffMillis)
    return createHikariDataSource(backoffSequence)
}


//fun migrate(ds: DataSource) {
//    val flyway = Flyway.configure().dataSource(ds).load()
//    flyway.migrate()
//}