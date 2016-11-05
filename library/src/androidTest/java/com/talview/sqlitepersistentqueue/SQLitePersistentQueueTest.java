package com.talview.sqlitepersistentqueue;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.talview.sqlitepersistentqueue.db.SQLiteQueueDbHelper;
import com.talview.sqlitepersistentqueue.db.SQLiteQueueTableManager;
import com.talview.sqlitepersistentqueue.db.sqlite_queue_contract.SQLiteQueueTable;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * A test class for the sqlite persistent queue
 */
@RunWith(AndroidJUnit4.class)
public class SQLitePersistentQueueTest {
    private QueueObjectConverter<String> mConverter = new QueueObjectConverter<String>() {
        @Override
        public String deserialize(String value) {
            return value;
        }

        @Override
        public String serialize(String queueObject) {
            return queueObject;
        }
    };

    private int mAddCallCounter = 0;
    private int mRemoveCallCounter = 0;
    private int mClearCallCounter = 0;

    private SQLiteBusSubscriber<String> mSubscriber = new SQLiteBusSubscriber<String>() {
        @Override
        public void onAdded(String object) {
            ++mAddCallCounter;
        }

        @Override
        public void onRemoved(String object) {
            ++mRemoveCallCounter;
        }

        @Override
        public void onCleared() {
            ++mClearCallCounter;
        }
    };

    private SQLitePersistentQueue<String> queue;

    @Before
    public void setUp() {
        Context appContext = InstrumentationRegistry.getTargetContext();
        queue = new SQLitePersistentQueue<>(appContext, mConverter);
    }

    @After
    public void tearDown() throws IOException {
        mAddCallCounter = 0;
        mRemoveCallCounter = 0;
        mClearCallCounter = 0;
        queue.clear();
        queue.close();
    }

    @Test
    public void testOffer_pushesItemToQueue() {
        queue.offer("test1");
        SQLiteQueueDbHelper helper = new SQLiteQueueDbHelper(InstrumentationRegistry.getTargetContext());
        Cursor c = helper.getReadableDatabase().rawQuery("SELECT * FROM " + SQLiteQueueTable.TABLE_NAME, null);
        c.moveToFirst();
        String value = c.getString(c.getColumnIndex(SQLiteQueueTable.COLUMN_NAME_VALUE));
        c.close();
        helper.close();
        assertEquals("test1", value);
    }

    @Test
    public void testIterator_IterateOnAllElements() {
        queue.offer("test1");
        queue.offer("test2");
        queue.offer("test3");
        queue.offer("test4");
        int i = 1;
        for (String item : queue) {
            assertEquals("test" + i, item);
            i++;
        }
        // i should be incremented 4 times
        assertEquals(5, i);
    }

    @Test
    public void testIterator_WhenQueueEmpty() {
        for (String ignored : queue) {
            fail("Must not enter this block since queue is empty");
        }
    }

    @Test
    public void testSize_returnCorrectSizeOfList() {
        queue.offer("1");
        queue.offer("2");
        int expectedSize = 2;
        assertEquals(expectedSize, queue.size());
    }

    @Test
    public void testIsEmpty_returnTrueWhenEmpty() {
        queue.clear();
        assertTrue(queue.isEmpty());
    }

    @Test
    public void testIsEmpty_returnFalseWhenNotEmpty() {
        queue.offer("1");
        assertFalse(queue.isEmpty());
    }

    @Test
    public void testContains_returnTrueWhenListContainsItem() {
        String itemToCheck = "1";
        queue.offer(itemToCheck);
        queue.offer("2");
        assertTrue(queue.contains(itemToCheck));
        assertTrue(queue.contains("2"));
    }

    @Test
    public void testContains_returnFalseWhenListDoesNotContainItem() {
        String itemThatDoesntExist = "15135";
        queue.offer("2");
        queue.offer("12");
        assertFalse(queue.contains(itemThatDoesntExist));
    }

    @SuppressLint("UseValueOf")
    @Test
    public void testContains_returnFalseWhenSerializationFails() {
        //noinspection UnnecessaryBoxing,SuspiciousMethodCalls
        assertFalse(queue.contains(new Integer(5)));
    }

    @Test
    public void testAdd_addsItemToList() {
        String itemToAdd = "13245";
        assertTrue(queue.add(itemToAdd));
        SQLiteQueueDbHelper helper = new SQLiteQueueDbHelper(InstrumentationRegistry.getTargetContext());
        Cursor c = helper.getReadableDatabase().rawQuery("SELECT * FROM " + SQLiteQueueTable.TABLE_NAME, null);
        c.moveToFirst();
        String value = c.getString(c.getColumnIndex(SQLiteQueueTable.COLUMN_NAME_VALUE));
        c.close();
        helper.close();
        assertEquals(itemToAdd, value);
    }

    @Test
    public void testRemove_removesItemFromList() {
        String itemToRemoveAndAdd = "12333";
        assertTrue(queue.add(itemToRemoveAndAdd));
        assertTrue(queue.remove(itemToRemoveAndAdd));
        assertFalse(queue.contains(itemToRemoveAndAdd));
    }

    @Test
    public void testRemove_itemNotInList_returnFalse() {
        String itemToRemove = "asdasd";
        queue.addAll(createListForTest());
        assertFalse(queue.remove(itemToRemove));
    }

    @Test
    public void testAddAll_AddsAllItemsFromCollection() {
        List<String> list = createListForTest();
        queue.addAll(list);
        assertTrue(queue.contains(value1()));
        assertTrue(queue.contains(value2()));
        assertTrue(queue.contains(value3()));
    }

    @Test
    public void testContainsAll_ListOfItems_MustReturnTrue() {
        List<String> list = createListForTest();
        queue.addAll(list);
        assertTrue(queue.containsAll(list));
    }

    @Test
    public void testContainsAll_ListOfItems_MustReturnFalse() {
        List<String> list = createListForTest();
        queue.addAll(list);
        list.add("this is not present in queue");
        assertFalse(queue.containsAll(list));
    }

    @Test
    public void removeAll_ListOfItems_MustRemoveAllItems() {
        // Arrange
        List<String> list = createListForTest();
        queue.addAll(list);
        queue.add("Something I wont remove");
        assertTrue(queue.removeAll(list));
        assertTrue(queue.contains("Something I wont remove"));
        assertEquals(1, queue.size());
    }

    @Test
    public void testPoll_ReturnsAndRemovesTheHeadOfQueue() {
        queue.addAll(createListForTest());
        assertEquals(value1(), queue.poll());
        assertFalse(queue.contains(value1()));
    }

    @Test
    public void testPoll_emptyQueue_mustReturnNull() {
        assertNull(queue.poll());
    }

    @Test
    public void testPeek_ReturnsHeadOfQueue() {
        queue.addAll(createListForTest());
        assertEquals(value1(), queue.peek());
    }

    @Test
    public void peek_ReturnNullWhenQueueIsEmpty() {
        assertNull(queue.peek());
    }

    @Test
    public void testRemove_throwsIfQueueIsEmpty() {
        try {
            queue.remove();
            fail("Must throw Exception where the queue is empty");
        } catch (NoSuchElementException ignore) {
        }
    }

    @Test
    public void testSubscriberOnAddedCalled_WhenAddingItem() {
        subscribe();
        List<String> list = createListForTest();
        queue.addAll(list);
        assertEquals(mAddCallCounter, list.size());
        unsubscribe();
    }

    @Test
    public void testSubscriberOnRemovedCalled_WhenRemovingItem() {
        subscribe();
        List<String> list = createListForTest();
        queue.addAll(list);
        queue.removeAll(list);
        assertEquals(mRemoveCallCounter, list.size());
        assertTrue(queue.isEmpty());
        unsubscribe();
    }

    @Test
    public void testSubscriberOnRemoveCalled_WhenPollingQueue() {
        subscribe();
        queue.offer(value1());
        queue.poll();
        assertEquals(mRemoveCallCounter, 1);
        assertEquals(mAddCallCounter, 1);
    }

    @Test
    public void testSubscriberOnClearCalled_WhenClearingQueue() {
        subscribe();
        queue.addAll(createListForTest());
        queue.clear();
        assertEquals(1, mClearCallCounter);
    }

    @Test
    public void testHasSubscriber_MustReturnTrueWhenSubscriberIsAdded() {
        subscribe();
        assertTrue(queue.getEventBus().hasSubscribers());
        unsubscribe();
        assertFalse(queue.getEventBus().hasSubscribers());
    }

    @Test
    public void testToArray_MustReturnArrayOfItemsInQueue() {
        queue.addAll(createListForTest());
        String[] a = queue.toArray(new String[0]);
        assertEquals(a[0], value1());
        assertEquals(a[1], value2());
        assertEquals(a[2], value3());
    }

    @Test
    public void testToArrayWithoutRunTimeType_MustReturnArrayOfItemsInQueue() {
        queue.addAll(createListForTest());
        Object[] a = queue.toArray();
        assertEquals(a[0], value1());
        assertEquals(a[1], value2());
        assertEquals(a[2], value3());
    }

    @Test
    public void testToArrayWithLargeArraySupplied() {
        queue.addAll(createListForTest());
        String[] a = queue.toArray(new String[5]);
        assertEquals(a[0], value1());
        assertEquals(a[1], value2());
        assertEquals(a[2], value3());
        assertNull(a[3]);
        assertNull(a[4]);
    }

    @Test
    public void testToArray_WhenSizeMethodReturnsLessThanActualSize() throws IOException {
        SQLiteQueueTableManager spyManager = Mockito.spy(new SQLiteQueueTableManager(
                new SQLiteQueueDbHelper(InstrumentationRegistry.getTargetContext())
        ));
        Mockito.when(spyManager.getCount()).thenReturn(2);
        SQLitePersistentQueue<String> spiedQueue = new SQLitePersistentQueue<>(mConverter, spyManager);
        spiedQueue.addAll(createListForTest());
        Object[] a = spiedQueue.toArray();
        assertEquals(a[0], value1());
        assertEquals(a[1], value2());
        assertEquals(a[2], value3());
        spiedQueue.clear();
        spiedQueue.close();
    }

    @Test
    public void testToArrayWithRunTimeType_WhenSizeMethodReturnsLessThanActualSize() throws IOException {
        SQLiteQueueTableManager spyManager = Mockito.spy(new SQLiteQueueTableManager(
                new SQLiteQueueDbHelper(InstrumentationRegistry.getTargetContext())
        ));
        Mockito.when(spyManager.getCount()).thenReturn(2);
        SQLitePersistentQueue<String> spiedQueue = new SQLitePersistentQueue<>(mConverter, spyManager);
        spiedQueue.addAll(createListForTest());
        String[] a = spiedQueue.toArray(new String[0]);
        assertEquals(a[0], value1());
        assertEquals(a[1], value2());
        assertEquals(a[2], value3());
        spiedQueue.clear();
        spiedQueue.close();
    }

    @Test
    public void testRetainAll_MustRetainOnlyDesiredElements() {
        queue.addAll(createListForTest());
        List<String> desiredElements = new ArrayList<>(3);
        desiredElements.add("desiredElement1");
        desiredElements.add("desiredElement2");
        desiredElements.add("desiredElement3");
        queue.retainAll(desiredElements);
        assertFalse(queue.contains(value1()));
        assertFalse(queue.contains(value2()));
        assertFalse(queue.contains(value3()));

        assertTrue(queue.contains("desiredElement1"));
        assertTrue(queue.contains("desiredElement2"));
        assertTrue(queue.contains("desiredElement3"));
    }

    @Test
    public void testRemove() {
        queue.addAll(createListForTest());
        // assert removed item is head
        assertEquals(queue.remove(), value1());
        // assert queue doesnt contain that item.
        assertFalse(queue.contains(value1()));
    }

    @Test
    public void testRemove_emptyQueue_mustThrow() {
        try {
            queue.remove();
            fail("Must throw when queue is empty and remove() is called");
        } catch (RuntimeException ignored) {
        }
    }

    @Test
    public void testElement_emptyQueue_mustThrow() {
        try {
            queue.element();
            fail("Must throw when queue is empty and element() is called");
        } catch (RuntimeException ignored) {
        }
    }

    @Test
    public void testElement_nonEmptyQueue_mustReturnHeadAndNotRemove() {
        queue.addAll(createListForTest());
        assertEquals(queue.element(), value1());
        assertTrue(queue.contains(value1()));
    }


    private void subscribe() {
        queue.getEventBus().subscribe(mSubscriber);
    }

    private void unsubscribe() {
        queue.getEventBus().unsubscribe(mSubscriber);
    }

    //region LIST HELPER
    private List<String> createListForTest() {
        List<String> list = new ArrayList<>();
        list.add(value1());
        list.add(value2());
        list.add(value3());
        return list;
    }

    private String value1() {
        return "value1";
    }

    private String value2() {
        return "value2";
    }

    private String value3() {
        return "value3";
    }
    //endregion
}
