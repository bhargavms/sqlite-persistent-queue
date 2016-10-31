package com.talview.sqlitepersistentqueue;

import org.junit.Test;

import static org.junit.Assert.*;

public class SQLiteBusPublisherTest {
    @Test
    public void publish_MustThrowForUnsupportedEventValue() throws Exception {
        SQLiteBusPublisher<String> publisher = new SQLiteBusPublisher<>();
        try {
            publisher.publish(1231, "blah");
            fail("Must throw exception for un supported int event values");
        } catch (IllegalArgumentException ignored) {
        }
    }

    @Test
    public void subscribe_MustNotSameSubscriberTwidce() throws Exception {
        SQLiteBusSubscriber<String> subscriber = new SQLiteBusSubscriber<String>() {
            @Override
            public void onAdded(String object) {

            }

            @Override
            public void onRemoved(String object) {

            }

            @Override
            public void onCleared() {

            }
        };
        SQLiteBusPublisher<String> publisher = new SQLiteBusPublisher<>();
        publisher.subscribe(subscriber);
        publisher.subscribe(subscriber);
        assertEquals(1, publisher.subsriberSize());
    }
}