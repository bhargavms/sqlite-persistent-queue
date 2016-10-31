package com.talview.sqlitepersistentqueue;

import android.support.test.InstrumentationRegistry;

import com.talview.sqlitepersistentqueue.db.SQLiteQueueDbHelper;
import com.talview.sqlitepersistentqueue.db.SQLiteQueueTableManager;

import org.junit.Test;

import java.util.Iterator;

import static org.junit.Assert.*;

public class SQLiteQueueIteratorTest {
    @Test
    public void next() throws Exception {
        SQLiteQueueDbHelper helper = new SQLiteQueueDbHelper(InstrumentationRegistry.getTargetContext());
        Iterator<String> it = new SQLiteQueueIterator<>(
                new SQLiteQueueTableManager(helper),
                new QueueObjectConverter<String>() {
                    @Override
                    public String deserialize(String value) {
                        return value;
                    }

                    @Override
                    public String serialize(String queueObject) {
                        return queueObject;
                    }
                }
        );
        assertNull(it.next());
        helper.close();
    }

}