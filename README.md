# Kotlin Exposed (SQL library) demo

This repository accompanies a blog post about Kotlin Exposed (Kotlin SQL library).

The application stores Actors and Movies in an SQL database and exposes them via a 
simple REST api. The REST API is built with [Javalin](https://javalin.io/).

There are two variants, one with H2 and one with Postgres.

### Running with H2

Run `MainWithH2.kt`. It will automatically:

 1. create an in-memory H2 database
 2. create the schema
 3. load test data
 4. start a API server at localhost:8080

### Running with Postgres

First start a Postgres database. If you have docker available, you can use:

    docker run --name exposed-db -p 5432:5432 -e POSTGRES_USER=exposed -e POSTGRES_PASSWORD=exposed -d postgres

Then run `MainWithPostgresAndHikari.kt`. It will:

 1. create a HikariCP datasource connecting to the postgres database
 2. create or update the schema
 3. load test data if not already present
 4. start a API server at localhost:8080

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
    

