# SQLite based Persistent Queue.
[![Build Status](https://travis-ci.org/bhargavms/sqlite-persistent-queue.svg?branch=master)](https://travis-ci.org/bhargavms/sqlite-persistent-queue)
[![Coverage Status](https://coveralls.io/repos/github/bhargavms/sqlite-persistent-queue/badge.svg?branch=master)](https://coveralls.io/github/bhargavms/sqlite-persistent-queue?branch=master)
[![Release](https://jitpack.io/v/bhargavms/sqlite-persistent-queue.svg)](https://jitpack.io/bhargavms/sqlite-persistent-queue)

> A Java Queue interface implementation that stores directly to SqliteDb

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

## Getting started

Add to your project level `build.gradle`'s `allprojects` block like this
```groovy
allprojects {
    repositories {
        maven { url "https://jitpack.io" }
    }
}
```
Next add to your module level (app) `build.gradle`'s dependencies block like this

```groovy
dependencies {
    compile 'com.github.bhargavms:sqlite-persistent-queue:1.0.0'
}
```

You're all set, Now you can start using the `SQLitePersistentQueue` class.

## How to use

 You need to implement the `QueueObjectConverter<T>` class (T is the type of the object you are storing in the Queue) **to decide how you want to serialize/deserialize objects** as objects are converted to strings before being saved in the database.

A Sample implementation of the `QueueObjectConverter` interface:
> Here I use Gson to convert objects to/from json strings

```java
public class GsonPayloadConverter implements QueueObjectConverter<Payload> {
    private final Gson gson;

    public GsonPayloadConverter(Gson gson) {
        this.gson = gson;
    }

    @Override
    public Payload deserialize(String value) {
        return gson.fromJson(value, Payload.class);
    }

    @Override
    public String serialize(Payload queueObject) {
        return gson.toJson(queueObject);
    }
}
```

 This implementation you should pass to the constructor of `SQLitePersistentQueue` class. The `SQLitePersistentQueue` uses this implementation to convert objects to/from strings before returning storing/retreiving from the SqliteDb.

Example:
```java
Gson gson = new Gson()
queue = new SQLitePersistentQueue<>(c, new GsonPayloadConverter(gson));
```

This instantiated queue you can use it like any other java queue as it implements the `java.util.Queue` interface.

##### Important

Call `queue.close()` when you are done using the queue to close the connection to the Database.

## TODO:
 - Make Sqlite writes in chunks by providing `save()`  api?
