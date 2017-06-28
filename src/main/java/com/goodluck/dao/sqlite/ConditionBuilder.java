package com.goodluck.dao.sqlite;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteQueryBuilder;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by zhangfei on 2017/4/29.
 */
public class ConditionBuilder<T extends BaseTable> {
    private final SQLiteDatabase database;

    private Class<T> tableClass;
    private String[] columns;
    private String whereClause;
    private String[] whereArgs;
    private String groupBy;
    private String having;
    private String orderBy;
    private Integer limitOffset;
    private Integer limitSize;
    private boolean distinct;

    public ConditionBuilder(SQLiteDatabase database) {
        this.database = database;
    }

    public ConditionBuilder<T> withTable(Class<T> tableClass) {
        this.tableClass = tableClass;
        return this;
    }

    public ConditionBuilder<T> withColumns(String... columns) {
        this.columns = columns;
        return this;
    }

    public ConditionBuilder<T> withWhere(String whereClause, Object... whereArgs) {
        this.whereClause = whereClause;
        this.whereArgs = new String[whereArgs.length];

        for (int i = 0; i < whereArgs.length; i++) {
            Object arg = whereArgs[i];

            if (arg instanceof String
                    || arg instanceof Integer
                    || arg instanceof Long
                    || arg instanceof Float
                    || arg instanceof Double) {
                this.whereArgs[i] = arg.toString();
            } else if (arg instanceof Boolean) {
                this.whereArgs[i] = Boolean.valueOf(arg.toString()) ? "1" : "0";
            } else {
                throw new SQLException(arg.toString() + " is not supported as where argument in SQLITE");
            }
        }

        return this;
    }

    public T applySearchById(long id) {
        this.whereClause = BaseTable._ID + "=?";
        this.whereArgs = new String[]{String.valueOf(id)};

        return applySearchFirst();
    }

    public int applyCount() {
        this.columns = BaseTable.COUNT_COLUMNS;

        Cursor c = applySearch();
        if (c == null) {
            throw new SQLiteException("Cannot create cursor object, database or columns may have error...");
        }

        try {
            if (c.moveToFirst()) {
                return c.getInt(0);
            } else {
                return 0;
            }
        } finally {
            c.close();
        }
    }

    public ConditionBuilder<T> withGroupBy(String groupBy) {
        this.groupBy = groupBy;
        return this;
    }

    public ConditionBuilder<T> withHaving(String having) {
        this.having = having;
        return this;
    }

    public ConditionBuilder<T> withOrderBy(String orderBy) {
        this.orderBy = orderBy;
        return this;
    }

    public ConditionBuilder<T> withLimit(int limitOffset, int limitSize) {
        this.limitOffset = limitOffset;
        this.limitSize = limitSize;
        return this;
    }

    public ConditionBuilder<T> withDistinct(boolean distinct) {
        this.distinct = distinct;
        return this;
    }

    /**
     * Apply search and return cursor as result
     *
     * @return query cursor
     */
    public Cursor applySearch() {
        String limit = null;
        if (limitOffset != null && limitSize != null) {
            limit = limitOffset + "," + limitSize;
        }

        if (TextUtils.isEmpty(orderBy)) {
            orderBy = ReflectTools.getDefaultOrderBy(tableClass);
        }

        String tableName = ReflectTools.getTableName(tableClass);
        String query = SQLiteQueryBuilder.buildQueryString(
                distinct, tableName, columns, whereClause,
                groupBy, having, orderBy, limit);
        return database.rawQuery(query, whereArgs);
    }

    /**
     * Apply search with condition and return list as result
     *
     * @return list of table class object as result
     */
    public List<T> applySearchAsList() {
        Cursor c = applySearch();
        List<T> entities = new ArrayList<>();
        try {
            while (c.moveToNext()) {
                T table = getContent(c, tableClass);
                if (table != null) {
                    entities.add(table);
                }
            }
        } finally {
            c.close();
        }
        return entities;
    }

    /**
     * Apply search first row with condition
     *
     * @return first item of result
     */
    public T applySearchFirst() {
        Cursor c = applySearch();

        if (c == null) {
            throw new SQLiteException("Cannot create cursor object, database or columns may have error...");
        }

        try {
            if (c.moveToFirst()) {
                return getContent(c, tableClass);
            } else {
                return null;
            }
        } finally {
            c.close();
        }
    }

    private T getContent(Cursor cursor, Class<T> tableClass) {
        try {
            T content = tableClass.newInstance();
            content.id = cursor.getLong(0);
            content.restore(cursor, columns);
            return content;
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Apply delete with condition
     *
     * @return count of delete rows
     */
    public int applyDelete() {
        String tableName = ReflectTools.getTableName(tableClass);
        int count = database.delete(tableName, whereClause, whereArgs);
        if (TextUtils.isEmpty(whereClause)) {
            resetPrimaryKeyIfNeed(tableName);
        }
        return count;
    }

    /**
     * Apply delete table record
     *
     * @param table table object
     * @return count of deleted row
     */
    public int applyDelete(T table) {
        return applyDeleteById(table.id);
    }

    /**
     * Apply delete with id
     *
     * @param id primary key id of record
     * @return count of deleted row
     */
    public int applyDeleteById(long id) {
        if (id == BaseTable.NOT_SAVED) {
            return 0;
        }

        this.whereClause = BaseTable._ID + " = ?";
        this.whereArgs = new String[]{String.valueOf(id)};

        checkModifiable(tableClass, "applyDeleteById");
        return applyDelete();
    }

    /**
     * Apply update record with condition
     *
     * @param values content values to be updated
     * @return count of updated rows
     */
    public int applyUpdate(ContentValues values) {
        if (values == null || values.size() == 0) {
            throw new SQLiteException("ContentValues is empty, nothing can be updated");
        }

        String tableName = ReflectTools.getTableName(tableClass);
        checkModifiable(tableClass, "applyUpdate");
        return database.update(tableName, values, whereClause, whereArgs);
    }

    /**
     * Apply update with table object
     *
     * @param table table object to update
     * @return count of updated row
     */
    public int applyUpdate(T table) {
        if (table == null) {
            throw new SQLException("table to update cannot be null");
        }

        this.whereClause = BaseTable._ID + " = ?";
        this.whereArgs = new String[]{String.valueOf(table.id)};
        ContentValues values = table.toContentValues();
        return applyUpdate(values);
    }

    /**
     * Reset primary key as zero when it's too large(if exceed Long.MAX_VALUE, exception will be throw)
     *
     * @param tableName table's name
     */
    private void resetPrimaryKeyIfNeed(String tableName) {
        if (TextUtils.isEmpty(tableName)) {
            return;
        }

        long maxLimit = Long.MAX_VALUE / 4 * 3;
        Cursor cursor = null;
        try {
            cursor = database.rawQuery("SELECT * FROM sqlite_sequence WHERE name = ?", new String[]{tableName});
            if (cursor != null && cursor.moveToNext()) {
                long seq = cursor.getLong(1);

                if (seq > maxLimit) {
                    database.execSQL("UPDATE sqlite_sequence SET seq = 0 WHERE name = ?", new String[]{tableName});
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * As we all known Table View is only used to search, its record cannot be updated or deleted.
     *
     * @param tableClass table class
     * @param actionDesc string flag
     */
    private void checkModifiable(Class<? extends BaseTable> tableClass, String actionDesc) {
        try {
            if (!tableClass.newInstance().isTable()) {
                throw new SQLiteException("Failed to " + actionDesc + " [" + tableClass.getSimpleName()
                        + "], since it is table view not table.");
            }
        } catch (InstantiationException e) {
            throw new SQLiteException(e.getMessage());
        } catch (IllegalAccessException e) {
            throw new SQLiteException(e.getMessage());
        }
    }
}
