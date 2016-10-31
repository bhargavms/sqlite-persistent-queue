package com.talview.sqlitepersistentqueue.db;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

import static org.junit.Assert.*;

/**
 * An instrumentation test for the sqlite queue table manager class.
 */
@RunWith(AndroidJUnit4.class)
public class SQLiteQueueTableManagerTest {

    private SQLiteQueueTableManager manager;

    @Before
    public void setUp() throws Exception {
        manager = getDbManager();
    }

    @After
    public void tearDown() throws Exception {
        manager.clear();
        manager.close();
    }

    @Test
    public void testInsert_MustInsertItem() throws IOException {
        manager.insert("test");
        String head = manager.removeHead();
        assertEquals("test", head);
    }

    @Test
    public void testRemoveHead_WhenDbIsEmpty() throws IOException {
        String result = manager.removeHead();
        assertNull(result);
    }

    @Test
    public void testContains_ReturnFalse_WhenEmpty() throws IOException {
        assertFalse(manager.contains("blah"));
    }

    @Test
    public void testContains_ReturnTrue_WhenContains() throws IOException {
        manager.insert("test1");
        manager.insert("test");
        assertTrue(manager.contains("test"));
        manager.insert("test2");
        assertTrue(manager.contains("test2"));
        assertTrue(manager.contains("test1"));
        manager.removeHead();
        assertFalse(manager.contains("test1"));
    }

    @Test
    public void testGetCount_GetRightNumberOfRows() throws IOException {
        manager.insert("test");
        manager.insert("test1");
        manager.insert("test2");
        manager.insert("test3");
        long count = manager.getCount();
        assertEquals(4, count);
        manager.removeHead();
        assertEquals(3, manager.getCount());
        manager.removeHead();
        assertEquals(2, manager.getCount());
        manager.clear();
        assertEquals(0, manager.getCount());
    }

    private SQLiteQueueTableManager getDbManager() {
        Context appContext = InstrumentationRegistry.getTargetContext();
        return new SQLiteQueueTableManager(new SQLiteQueueDbHelper(appContext));
    }

    @Test
    public void testRemoveHead_MustRemoveTheFirstInsertedItem() throws IOException {
        manager.insert("test");
        manager.insert("test1");
        manager.insert("test2");
        manager.insert("test3");
        String head = manager.removeHead();
        assertEquals("test", head);
        head = manager.removeHead();
        assertEquals("test1", head);
        head = manager.removeHead();
        assertEquals("test2", head);
        int count = manager.clear();
        assertEquals(1, count);
    }

    @Test
    public void testGet_MustReturnNullWhenIdDoesntExist() {
        assertNull(manager.get(0));
    }

    @Test
    public void testRemove_MustReturnNullWhenDbIsEmpty() {
        assertNull(manager.remove("this doesnt exist"));
    }
}
