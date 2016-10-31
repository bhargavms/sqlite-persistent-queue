package com.talview.sqlitepersistentqueue;

/**
 * The bus that takes care of subscriptions publishing for sqlite persistent queue.
 */
public class SQLitePersistentQueueBus<T> {
    private final SQLiteBusPublisher<T> publisher = new SQLiteBusPublisher<>();

    SQLitePersistentQueueBus() {
    }

    void onAdded(T object) {
        publisher.publish(SQLiteBusPublisher.EVENT_ADDED, object);
    }

    void onRemoved(T object) {
        publisher.publish(SQLiteBusPublisher.EVENT_REMOVED, object);
    }

    void onCleared() {
        publisher.publish(SQLiteBusPublisher.EVENT_CLEARED, null);
    }

    public void subscribe(SQLiteBusSubscriber<T> subscriber) {
        publisher.subscribe(subscriber);
    }

    public void unsubscribe(SQLiteBusSubscriber<T> subscriber) {
        publisher.unsubscribe(subscriber);
    }

    public boolean hasSubscribers() {
        return publisher.hasSubscribers();
    }
}
