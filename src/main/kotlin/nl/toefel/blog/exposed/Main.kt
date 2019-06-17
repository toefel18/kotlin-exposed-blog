package nl.toefel.blog.exposed

import nl.toefel.blog.exposed.Cities.id
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val log: Logger = LoggerFactory.getLogger("Main")

object Users : Table() {
    val id = varchar("id", 10).primaryKey() // Column<String>
    val name = varchar("name", length = 50) // Column<String>
    val cityId = (integer("city_id") references Cities.id).nullable() // Column<Int?>
    val alternateCityId = integer("alternate_city_id").nullable()
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

    log.info("Inserting some cities and a user")
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
            it[alternateCityId] = amsterdam[Cities.id]
        }
    }






    transaction {
        log.info("There are ${Cities.selectAll().count()} cities")

        log.info("All cities:")
        Cities.selectAll().forEach {
            log.info("${it[Cities.name]} with ${it[Cities.inhabitants]} inhabitants")
        }

        log.info("Cities with more inhabitants than 100000")
        Cities.select {Cities.inhabitants greater 100000L }.forEach {
            log.info(it[Cities.name])
        }
    }

    log.info("Fetching all city names and returning it from the transaction as a list")

    val cityNames = transaction {
        Cities.slice(Cities.name)
            .selectAll()
            .orderBy(Cities.name, SortOrder.DESC)
            .map { it[Cities.name] }
    }
    log.info("City names in reverse order: $cityNames")






    log.info("Mapping to a domain class")
    transaction {
        data class CityRecord(val id: Int, val name: String, val size: Long)

        Cities.select{ Cities.name eq "Utrecht"}
            .map { CityRecord(it[Cities.id], it[Cities.name], it[Cities.inhabitants]) }
            .forEach {log.info("City record of utrecht: $it") }
    }

    log.info("Batch inserting users")
    data class UserRecord(val id: String, val name: String, val cityName: String)
    val newUsers = listOf(
        UserRecord("1000", "Sjaak", "Amsterdam"),
        UserRecord("1001", "Kees", "Utrecht"),
        UserRecord("1002", "Marja", "Amsterdam"),
        UserRecord("1003", "Ronnie", "Unknown"),
        UserRecord("1004", "Majo", "Rotterdam")
    )
    transaction {
        val x = Users.batchInsert(newUsers) { user ->
            this[Users.id] = user.id
            this[Users.name] = user.name
            this[Users.cityId] = Cities.slice(Cities.id)
                .select { Cities.name eq user.cityName }
                .map { it[Cities.id] }
                .firstOrNull()
        }
    }


    log.info("Trying to insert user with missing field name")
    transaction {
        try {
            Users.insert {
                it[id] = "123"
                it[cityId] = 1
            }
        } catch (ex: Exception) {
            println(ex.message)
        }
    }


    log.info("Selecting all users")
    transaction {
        Users.selectAll().forEach {
            log.info("id=${it[Users.id]} name=${it[Users.name]} cityId=${it[Users.cityId]}")
        }
    }



    log.info("Selecting all users and their city using JOIN")
    transaction {
        (Users leftJoin Cities)
            .slice(Users.name, Cities.name)
            .select { Cities.name.isNotNull()}
            .forEach { log.info("User ${it[Users.name]} lives in ${it[Cities.name]}") }
    }



    log.info("Joining based on a column that is not defined as a foreign key")
    transaction {
        // only user Chris has the column alternateCityId set to an id of a city, see line 58
        Users.join(Cities, JoinType.LEFT, onColumn = Users.alternateCityId, otherColumn = Cities.id)
            .slice(Users.name, Cities.name)
            .select { Cities.name.isNotNull()}
            .forEach { log.info("User ${it[Users.name]} has as alternate city: ${it[Cities.name]}") }
    }



    log.info("Nested transactions")
    transaction {
        val groningenId = Cities.insert { it[name] = "Groningen"; it[inhabitants] = 740000 } get id
        transaction {
            Users.insert {
                it[id] = "12356"
                it[name] = "Groninger"
                it[cityId] = groningenId
            }
        }
        rollback()      // rolls back the groningen and user inserts~!
        // this will still be inserted!
        Cities.insert { it[name] = "Groningen2"; it[inhabitants] = 750000 } get id
    }


}