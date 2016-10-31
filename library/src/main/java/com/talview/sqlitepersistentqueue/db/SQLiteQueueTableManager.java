package com.talview.sqlitepersistentqueue.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.talview.sqlitepersistentqueue.db.sqlite_queue_contract.SQLiteQueueTable;

import java.io.Closeable;
import java.io.IOException;

/**
 * A class that wraps all the db querying code.
 */
public class SQLiteQueueTableManager implements Closeable {
    private final SQLiteDatabase mDb;

    public SQLiteQueueTableManager(SQLiteQueueDbHelper queueDbHelper) {
        this.mDb = queueDbHelper.getWritableDatabase();
    }

    public long insert(String value) {
        ContentValues cv = new ContentValues();
        cv.put(SQLiteQueueTable.COLUMN_NAME_VALUE, value);
        return mDb.insert(SQLiteQueueTable.TABLE_NAME, null, cv);
    }

    public int clear() {
        return mDb.delete(SQLiteQueueTable.TABLE_NAME, "1", null);
    }

    public int getCount() {
        Cursor c = mDb.query(SQLiteQueueTable.TABLE_NAME, new String[]{SQLiteQueueTable._ID},
                "1", null, null, null, null);
        int count = c.getCount();
        c.close();
        return count;
    }

//    public String getNext(long fromThis) {
//        String[] projection = {
//                SQLiteQueueTable.COLUMN_NAME_VALUE
//        };
//        String selection = SQLiteQueueTable._ID + " = ?";
//        String[] selectionArgs = {
//                String.valueOf(getNextId(fromThis))
//        };
//        Cursor c = mDb.query(SQLiteQueueTable.TABLE_NAME, projection,
//                selection, selectionArgs, null, null, null);
//        if (c.getCount() == 0) {
//            c.close();
//            return null;
//        }
//        String value = c.getString(0);
//        c.close();
//        return value;
//    }

    public String get(long id) {
        String[] projection = {
                SQLiteQueueTable.COLUMN_NAME_VALUE
        };
        String selection = SQLiteQueueTable._ID + " = ?";
        String[] selectionArgs = {
                String.valueOf(id)
        };
        Cursor c = mDb.query(SQLiteQueueTable.TABLE_NAME, projection,
                selection, selectionArgs, null, null, null);
        c.moveToFirst();
        if (c.getCount() == 0) {
            c.close();
            return null;
        }
        String value = c.getString(0);
        c.close();
        return value;
    }

    public String getHead() {
        if (getCount() == 0)
            return null;
        long minId = getMinId();
        return get(minId);
    }

    public long getNextId(long fromThis) {
        String sql = "SELECT MIN(" + SQLiteQueueTable._ID + ")"
                + " FROM " + SQLiteQueueTable.TABLE_NAME + " WHERE " +
                SQLiteQueueTable._ID + " > " + fromThis;
        Cursor c = mDb.rawQuery(sql, null);
        c.moveToFirst();
        long nearestId = c.getLong(0);
        c.close();
        return nearestId;
    }

    public boolean contains(String value) {
        String[] columns = {
                SQLiteQueueTable.COLUMN_NAME_VALUE,
        };
        String selection = SQLiteQueueTable.COLUMN_NAME_VALUE + " = ?";
        String[] selectionArgs = {
                value
        };
        Cursor c = mDb.query(SQLiteQueueTable.TABLE_NAME,
                columns, selection, selectionArgs, null, null, null);
        boolean contains = c.getCount() > 0;
        c.close();
        return contains;
    }

    public String removeHead() {
        String[] projection = {
                SQLiteQueueTable.COLUMN_NAME_VALUE
        };

        String selection = SQLiteQueueTable._ID + " = ?";
        String[] selectionArgs = {
                String.valueOf(getMinId())
        };

        Cursor c = mDb.query(
                SQLiteQueueTable.TABLE_NAME,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                null
        );
        c.moveToFirst();
        if (c.getCount() == 0) {
            c.close();
            return null;
        }
        String value = c.getString(c.getColumnIndex(SQLiteQueueTable.COLUMN_NAME_VALUE));
        c.close();
        mDb.delete(SQLiteQueueTable.TABLE_NAME, selection, selectionArgs);
        return value;
    }

    public String remove(String s) {
        String selection = SQLiteQueueTable.COLUMN_NAME_VALUE + " = ?";
        String[] selectionArgs = {
                s
        };
        int count = mDb.delete(SQLiteQueueTable.TABLE_NAME, selection, selectionArgs);
        if (count > 0)
            return s;
        else
            return null;
    }

    private long getMinId() {
        String sql = "SELECT " + "MIN(" + SQLiteQueueTable._ID + ")"
                + " FROM " + SQLiteQueueTable.TABLE_NAME;
        Cursor c = mDb.rawQuery(sql, null);
        c.moveToFirst();
        long minId = c.getLong(0);
        c.close();
        return minId;
    }

    public long getMaxId() {
        String sql = "SELECT " + "MAX(" + SQLiteQueueTable._ID + ")"
                + " FROM " + SQLiteQueueTable.TABLE_NAME;
        Cursor c = mDb.rawQuery(sql, null);
        c.moveToFirst();
        long maxId = c.getLong(0);
        c.close();
        return maxId;
    }

    @Override
    public void close() throws IOException {
        mDb.close();
    }
}
