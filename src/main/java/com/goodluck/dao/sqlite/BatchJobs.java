package com.goodluck.dao.sqlite;

import android.content.ContentValues;

import java.util.ArrayList;
import java.util.List;

/**
 * A container to contain more than one CRUD jobs, and should be executed by
 * {@link DBUtils#applyBatchJobs(BatchJobs)}.
 * 
 * @author zf08526
 * 
 */
public final class BatchJobs {
	private final ArrayList<SQL> batchJobs;

	public BatchJobs() {
		this.batchJobs = new ArrayList<>();
	}

	public <T extends BaseTable> void addInsertJob(T table) {
		batchJobs.add(SQLBuilder.buildInsertSQL(table));
	}

	public <T extends BaseTable> void addInsertJob(List<T> tables) {
		for (T table : tables) {
			batchJobs.add(SQLBuilder.buildInsertSQL(table));
		}
	}

	public <T extends BaseTable> void addUpdateJob(Class<T> tableClass, long id, ContentValues values) {
		batchJobs.add(SQLBuilder.buildUpdateSQL(tableClass, id, values));
	}

	public <T extends BaseTable> void addUpdateJob(Class<T> tableClass, T table) {
		batchJobs.add(SQLBuilder.buildUpdateSQL(tableClass, table.toContentValues(), BaseTable._ID + "=?", table.id));
	}

	public <T extends BaseTable> void addUpdateJob(Class<T> tableClass, ContentValues values, String where, Object... whereArgs) {
		batchJobs.add(SQLBuilder.buildUpdateSQL(tableClass, values, where, whereArgs));
	}

	public <T extends BaseTable> void addDeleteJob(T table) {
		batchJobs.add(SQLBuilder.buildDeleteSQL(table));
	}

	public <T extends BaseTable> void addDeleteJob(Class<T> tableClass) {
		batchJobs.add(SQLBuilder.buildDeleteSQL(tableClass));
	}

	public <T extends BaseTable> void addDeleteJob(Class<T> tableClass, long id) {
		batchJobs.add(SQLBuilder.buildDeleteSQL(tableClass, id));
	}

	public <T extends BaseTable> void addDeleteJob(Class<T> tableClass, String where, Object... whereArgs) {
		batchJobs.add(SQLBuilder.buildDeleteSQL(tableClass, where, whereArgs));
	}

	public ArrayList<SQL> getBatchJobs() {
		return batchJobs;
	}
}