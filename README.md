# kotlin-exposed-blog
Kotlin exposed example repo 


## Starting a local database 

    docker run --name exposed-db -p 5432:5432 -e POSTGRES_USER=exposed -e POSTGRES_PASSWORD=exposed -d postgres