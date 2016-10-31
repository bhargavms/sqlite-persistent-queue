package com.talview.sqlitepersistentqueue.db.sqlite_queue_contract;

import android.provider.BaseColumns;

/**
 * The sqlite queue table.
 */
public class SQLiteQueueTable implements BaseColumns {
    public static final String TABLE_NAME = "sqlitequeue";
    public static final String COLUMN_NAME_VALUE = "value";
}
