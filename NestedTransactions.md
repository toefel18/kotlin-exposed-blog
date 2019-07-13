#### Nested transactions details 

Transaction blocks can be nested:

```kotlin
transaction {
    Actors.insert {
      it[firstName] = "Alice"
    }
    
    transaction {
        Actors.insert {
          it[firstName] = "Bruce"
        }
    }  // 1  
     
       // 2
     
    Actors.insert {
      it[firstName] = "Jeff"
    }
}      // 3 
```
In the happy flow:
 - At point 1, nothing gets committed yet. 
 - At point 3, Alice, Bruce and Jeff are all committed.
 
For this to work correctly, the following configuration is required:
```kotlin
val db = Database.connect(...)
db.useNestedTransactions = true 
```
If `db.useNestedTransactions` is false then the code sample above would commit Alice and Bruce at point 1. Any following 
rollbacks() do not work. This appears to be a bug in the newest version: [Jetbrains/Exposed/issues/605](https://github.com/JetBrains/Exposed/issues/605).

