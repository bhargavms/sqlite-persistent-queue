package com.talview.sqlitepersistentqueue;

import android.content.Context;
import android.support.annotation.NonNull;

import com.talview.sqlitepersistentqueue.db.SQLiteQueueDbHelper;
import com.talview.sqlitepersistentqueue.db.SQLiteQueueTableManager;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Queue;

/**
 * A persistent queue implementation using Sqlite.
 */
public class SQLitePersistentQueue<E> implements Queue<E>, Closeable {
    private QueueObjectConverter<E> mConverter;
    private SQLiteQueueTableManager mDbManager;
    private SQLitePersistentQueueBus<E> mQueueEventBus;
    private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

    public SQLitePersistentQueue(@NonNull Context context,
                                 @NonNull QueueObjectConverter<E> objectConverter) {
        Context mAppContext = context.getApplicationContext();
        mConverter = objectConverter;
        mDbManager = new SQLiteQueueTableManager(new SQLiteQueueDbHelper(mAppContext));
        mQueueEventBus = new SQLitePersistentQueueBus<>();
    }

    public SQLitePersistentQueueBus<E> getEventBus() {
        return mQueueEventBus;
    }

    @Override
    public int size() {
        return mDbManager.getCount();
    }

    @Override
    public boolean isEmpty() {
        return mDbManager.getCount() == 0;
    }

    @Override
    public boolean contains(Object o) {
        try {
            @SuppressWarnings("unchecked")
            String converted = mConverter.serialize((E) o);
            return isContains(converted);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isContains(String converted) {
        return mDbManager.contains(converted);
    }

    @NonNull
    @Override
    public Iterator<E> iterator() {
        return new SQLiteQueueIterator<>(mDbManager, mConverter);
    }

    @NonNull
    @Override
    public Object[] toArray() {
        // Estimate size of array; be prepared to see more or fewer elements
        Object[] r = new Object[size()];
        Iterator<E> it = iterator();
        for (int i = 0; i < r.length; i++) {
            if (!it.hasNext()) // fewer elements than expected
                return Arrays.copyOf(r, i);
            r[i] = it.next();
        }
        return it.hasNext() ? finishToArray(r, it) : r;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T> T[] toArray(@NonNull T[] a) {
        // Estimate size of array; be prepared to see more or fewer elements
        int size = size();
        T[] r = a.length >= size
                ? a
                : (T[]) Array.newInstance(a.getClass().getComponentType(), size);
        Iterator<E> it = iterator();

        for (int i = 0; i < r.length; i++) {
            if (!it.hasNext()) { // fewer elements than expected
                if (a != r)
                    return Arrays.copyOf(r, i);
                r[i] = null; // null-terminate
                return r;
            }
            r[i] = (T) it.next();
        }
        return it.hasNext() ? finishToArray(r, it) : r;
    }

    private static <T> T[] finishToArray(T[] r, Iterator<?> it) {
        int i = r.length;
        while (it.hasNext()) {
            int cap = r.length;
            if (i == cap) {
                int newCap = cap + (cap >> 1) + 1;
                // overflow-conscious code
                if (newCap - MAX_ARRAY_SIZE > 0)
                    newCap = hugeCapacity(cap + 1);
                r = Arrays.copyOf(r, newCap);
            }
            r[i++] = (T) it.next();
        }
        // trim if overallocated
        return (i == r.length) ? r : Arrays.copyOf(r, i);
    }

    private static int hugeCapacity(int minCapacity) {
        if (minCapacity < 0) // overflow
            throw new OutOfMemoryError
                    ("Required array size too large");
        return (minCapacity > MAX_ARRAY_SIZE) ?
                Integer.MAX_VALUE :
                MAX_ARRAY_SIZE;
    }

    @Override
    public boolean add(E e) {
        long rowId = mDbManager.insert(mConverter.serialize(e));
        if (rowId == -1) {
            throw new RuntimeException("Failed to add element e = " + e.toString());
        }
        mQueueEventBus.onAdded(e);
        return true;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean remove(Object o) {
        if (contains(o)) {
            E e = (E) o;
            String converted = mConverter.serialize(e);
            String deletedItem = mDbManager.remove(converted);
            if (deletedItem != null) {
                mQueueEventBus.onRemoved(e);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean containsAll(@NonNull Collection<?> c) {
        for (Object item : c) {
            if (!contains(item))
                return false;
        }
        return true;
    }

    @Override
    public boolean addAll(@NonNull Collection<? extends E> c) {
        for (E o : c) {
            if (!add(o)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean removeAll(@NonNull Collection<?> c) {
        for (Object o : c) {
            if (!remove(o)) {
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean retainAll(@NonNull Collection<?> c) {
        for (Object o :
                c) {
            if (!contains(o)) {
                add((E) o);
            }
        }
        for (E e : this) {
            if (!c.contains(e)) {
                remove(e);
            }
        }
        return true;
    }

    @Override
    public void clear() {
        mDbManager.clear();
        mQueueEventBus.onCleared();
    }

    @Override
    public boolean offer(E e) {
        long rowId = mDbManager.insert(mConverter.serialize(e));
        if (rowId != -1) {
            mQueueEventBus.onAdded(e);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public E remove() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }
        String s = mDbManager.removeHead();
        E e = mConverter.deserialize(s);
        mQueueEventBus.onRemoved(e);
        return e;
    }

    @Override
    public E poll() {
        if (isEmpty())
            return null;
        String s = mDbManager.removeHead();
        E e = mConverter.deserialize(s);
        mQueueEventBus.onRemoved(e);
        return e;
    }

    @Override
    public E element() {
        if (isEmpty()) {
            throw new RuntimeException("Queue is empty");
        }
        String s = mDbManager.getHead();
        return mConverter.deserialize(s);
    }

    @Override
    public E peek() {
        String s = mDbManager.getHead();
        if (s == null)
            return null;
        return mConverter.deserialize(s);
    }

    @Override
    public void close() throws IOException {
        mDbManager.close();
    }
}
