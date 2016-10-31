package com.talview.sqlitepersistentqueue;

import com.talview.sqlitepersistentqueue.db.SQLiteQueueTableManager;

import java.util.Iterator;

/**
 * An iterator to iterate through the sqlite queue Db records.
 */
class SQLiteQueueIterator<T> implements Iterator<T> {
    private SQLiteQueueTableManager mDbManager;
    private long mCurrentId;
    private QueueObjectConverter<T> mConverter;

    SQLiteQueueIterator(SQLiteQueueTableManager manager,
                        QueueObjectConverter<T> converter) {
        mDbManager = manager;
        mCurrentId = -1;
        mConverter = converter;
    }

    @Override
    public boolean hasNext() {
        return mDbManager.getCount() > 0 && mCurrentId < mDbManager.getMaxId();
    }

    @Override
    public T next() {
        mCurrentId = mDbManager.getNextId(mCurrentId);
        String next = mDbManager.get(mCurrentId);
        if (next == null)
            return null;
        return mConverter.deserialize(next);
    }
}
