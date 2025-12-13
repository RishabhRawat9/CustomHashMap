# Custom HashMap

This project is a custom implementation of a thread-safe HashMap in Java. It is designed to be a learning exercise to understand the internal workings of a HashMap and multithreading in java.

## Features

- **Thread-Safe**: The `Jmap` class is thread-safe. It uses bucket-level locking to allow multiple threads to access the map concurrently. This is more efficient than locking the entire map for every operation.
- **Automatic Resizing**: The map automatically resizes itself when the number of elements exceeds the load factor. This is done to maintain a constant time complexity for the basic operations like `put`, `get`, and `remove`.
- **Key-Value Store**: The `KVStore` class provides a simple key-value store that uses the `Jmap` as its underlying data structure. It provides an interactive command-line interface to perform `put`, `get`, and `delete` operations.
- **Persistence**: The `KVStore` class also provides a simple persistence mechanism. It logs all the `put` and `delete` operations to a log file. When the application is started, it rebuilds the in-memory key-value store from the log file.

## Methods

- `PUT`: To add a new key-value pair.
- `GET`: To retrieve the value for a given key.
- `DELETE`: To remove a key-value pair.
