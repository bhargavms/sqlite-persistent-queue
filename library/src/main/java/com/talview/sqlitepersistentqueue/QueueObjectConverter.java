package com.talview.sqlitepersistentqueue;

/**
 * Created by talview23 on 10/10/16.
 */

public interface QueueObjectConverter<T> {
    T deserialize(String value);

    String serialize(T queueObject);
}
