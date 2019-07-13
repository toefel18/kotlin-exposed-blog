# Kotlin Exposed (SQL library) demo

This repository accompanies a blog post about 
[Kotlin Exposed (Kotlin SQL library)](https://github.com/JetBrains/Exposed).

The application stores Actors and Movies in an SQL database and exposes them via a 
simple REST api. The REST API is built with [Javalin](https://javalin.io/).

There are two variants, one with H2 and one with Postgres.

### Running with H2

Run [MainWithH2.kt](src/main/kotlin/nl/toefel/blog/exposed/MainWithH2.kt). It will automatically:

 1. create an in-memory H2 database
 2. create the schema
 3. load test data
 4. start a API server at localhost:8080

### Running with Postgres

First start a Postgres database. If you have docker available, you can use:

    docker run --name exposed-db -p 5432:5432 -e POSTGRES_USER=exposed -e POSTGRES_PASSWORD=exposed -d postgres

Then run [MainWithPostgresAndHikari](src/main/kotlin/nl/toefel/blog/exposed/MainWithPostgresAndHikari.kt). It will:

 1. create a HikariCP datasource connecting to the postgres database
 2. create or update the schema
 3. load test data if not already present
 4. start a API server at localhost:8080

# Reading Code

Start by looking at how the database tables are described [in code](src/main/kotlin/nl/toefel/blog/exposed/db/)

 * [Actors.kt](src/main/kotlin/nl/toefel/blog/exposed/db/Actors.kt)
 * [Movies.kt](src/main/kotlin/nl/toefel/blog/exposed/db/Movies.kt)
 * [ActorsInMovies.kt](src/main/kotlin/nl/toefel/blog/exposed/db/ActorsInMovies.kt)

Then look how the database is queried using the Kotlin Exposed DSL in the Router:

[Router.kt](src/main/kotlin/nl/toefel/blog/exposed/rest/Router.kt)

To interact with the database, simply start a transaction block:

```kotlin
val actorCount = transaction {
    Actors.selectAll().count()
}
```

`transaction` uses the connection that was configured in [MainWithH2.kt](src/main/kotlin/nl/toefel/blog/exposed/MainWithH2.kt)
or [MainWithPostgresAndHikari](src/main/kotlin/nl/toefel/blog/exposed/MainWithPostgresAndHikari.kt).
  
You can query as much as you want within a transaction block, when it goes out of scope without
an error, it will automatically `commit()`. Leaving the scope with an exception automatically 
triggers a `rollback()`. 

The last statement of the `transaction` is returned, as in the example. 

Transaction blocks can be nested. If this happens, then the outer block controls the `commit()` 
or `rollback()`.

```kotlin

transaction {
    val count = Actors.selectAll().count()  // returns 5
    transaction {
        Actors.insert {
          it[firstName] = "bruce"
        }
    }  // does not automatically commit because of the outer transaction 
    
    rollback() // reverts the insert!
}

```



# Using the REST apis
When started, you can use these URL's to interact with it:

    # fetch all actors
    curl http://localhost:8080/actors
    
    # fetch all actors with first name Angelina
    curl http://localhost:8080/actors?firstName=Angelina
    
    # add an actor
    curl -X POST http://localhost:8080/actors -H 'application/json' \
       -d '{"firstName":"Ousmane","lastName":"Dembele","dateOfBirth":"1975-05-10"}' 
    
    # delete an actor
    curl -X DELETE http://localhost:8080/actors/2
    
    
    
    # fetch all movies
    curl http://localhost:8080/movies
    
    # fetch a specific movie to see which actors are in it
    curl http://localhost:8080/movies/2
    

