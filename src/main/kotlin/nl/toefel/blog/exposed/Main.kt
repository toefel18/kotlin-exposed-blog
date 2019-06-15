package nl.toefel.blog.exposed

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val log: Logger = LoggerFactory.getLogger("Main")

object Users : Table() {
    val id = varchar("id", 10).primaryKey() // Column<String>
    val name = varchar("name", length = 50) // Column<String>
    val cityId = (integer("city_id") references Cities.id).nullable() // Column<Int?>
    val randomIdForRandomJoin = integer("random_id").nullable()
}

object Cities : Table() {
    val id = integer("id").autoIncrement().primaryKey() // Column<Int>
    val name = varchar("name", 60) // Column<String>
    val inhabitants = long("inhabitants")
}

fun main() {
    log.info("creating datasource and connect Exposed to it")
    val datasource = createHikariDataSource()
    Database.connect(datasource)

    log.info("Dropping and creating tables")
    transaction {
        SchemaUtils.drop(Users, Cities)
        SchemaUtils.create(Cities, Users)
    }

    log.info("Inserting cities: amsterdam and utrecht and a user toefel")
    transaction {
        log.info("inserting amsterdam")
        val amsterdam = Cities.insert {
            it[name] = "Amsterdam"
            it[inhabitants] = 300000
        }

        log.info("inserting utrecht")
        val utrecht = Cities.insert {
            it[name] = "Utrecht"
            it[inhabitants] = 200000
        }

        Cities.insert { it[name] = "Eindhoven"; it[inhabitants] = 120000 }
        Cities.insert { it[name] = "Rotterdam"; it[inhabitants] = 220000 }
        Cities.insert { it[name] = "Klinge"; it[inhabitants] = 50000 }
        Cities.insert { it[name] = "Vlissingen"; it[inhabitants] = 60000 }

        log.info("inserting Chris")
        Users.insert {
            it[id] = "1"
            it[name] = "Chris"
            it[cityId] = utrecht[Cities.id]
            it[randomIdForRandomJoin] = utrecht[Cities.id]
        }
    }


}