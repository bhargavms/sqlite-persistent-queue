package com.talview.sqlitepersistentqueue;

import android.support.annotation.VisibleForTesting;

import java.util.ArrayList;
import java.util.List;

/**
 * A class used to publish sqlite change event.
 */
class SQLiteBusPublisher<T> {
    private List<SQLiteBusSubscriber<T>> subscribers = new ArrayList<>();
    static final int EVENT_ADDED = 1002024;
    static final int EVENT_REMOVED = 1002025;
    static final int EVENT_CLEARED = 1002026;

    void publish(int event, T object) {
        switch (event) {
            case EVENT_ADDED:
                for (SQLiteBusSubscriber<T> subscriber : subscribers) {
                    subscriber.onAdded(object);
                }
                break;
            case EVENT_REMOVED:
                for (SQLiteBusSubscriber<T> subscriber : subscribers) {
                    subscriber.onRemoved(object);
                }
                break;
            case EVENT_CLEARED:
                for (SQLiteBusSubscriber<T> subscriber : subscribers) {
                    subscriber.onCleared();
                }
                break;
            default:
                throw new IllegalArgumentException("Un supported event value");
        }
    }

    void subscribe(SQLiteBusSubscriber<T> subscriber) {
        if (!subscribers.contains(subscriber))
            subscribers.add(subscriber);
    }

    void unsubscribe(SQLiteBusSubscriber<T> subscriber) {
        subscribers.remove(subscriber);
    }

    @VisibleForTesting
    int subsriberSize() {
        return subscribers.size();
    }

    boolean hasSubscribers() {
        return !subscribers.isEmpty();
    }
}
