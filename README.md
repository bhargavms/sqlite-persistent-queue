# SQLite based Persistent Queue.

A Java Queue interface implementation that stores directly to SqliteDb

## How it works

 * Implement the `QueueObjectConverter<T>` interface do decide how you want to serialize and
   deserialize the objects that you want to push to the Queue.

 * The `SQLitePersistentQueue` class uses the above implementation to serialize objects before storing
   it the Database.

 * Upon calling `poll()` or `remove()` on the `SQLitePersistentQueue` the item with the lowest primary
   key value (since the primary key is auto-incrementing) is removed from the Queue and returned. This
   co-incidentally is the HEAD of the queue (as queue is FIFO based).

 * Please remember each instance of SQLitePersistentQueue creates a new connection to the Database.
   I have kept the freedom of how many connections to be made to DB to the developer. The ideal usage
   for new developers would be to hold only a single instance of the `SQLitePersistentQueue` class
   in your Application class, and retrieve this wherever you want. Also **DO NOT** forget to call `close()`
   to close the connection to the Database once you are finished using the Queue.
