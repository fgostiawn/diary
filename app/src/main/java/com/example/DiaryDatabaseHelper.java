package com.example;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public class DiaryDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "diary_notebook.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_NAME = "diary_entries";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_CONTENT = "content";
    public static final String COLUMN_TIMESTAMP = "timestamp";
    public static final String COLUMN_PHOTO_PATH = "photo_path";
    public static final String COLUMN_LATITUDE = "latitude";
    public static final String COLUMN_LONGITUDE = "longitude";
    public static final String COLUMN_ADDRESS = "address";
    public static final String COLUMN_MOOD = "mood";

    public DiaryDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " ("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_TITLE + " TEXT NOT NULL, "
                + COLUMN_CONTENT + " TEXT NOT NULL, "
                + COLUMN_TIMESTAMP + " INTEGER NOT NULL, "
                + COLUMN_PHOTO_PATH + " TEXT, "
                + COLUMN_LATITUDE + " REAL NOT NULL, "
                + COLUMN_LONGITUDE + " REAL NOT NULL, "
                + COLUMN_ADDRESS + " TEXT, "
                + COLUMN_MOOD + " TEXT"
                + ")";
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    // CREATE - Insert a new diary entry
    public long insertDiary(Diary diary) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TITLE, diary.getTitle());
        values.put(COLUMN_CONTENT, diary.getContent());
        values.put(COLUMN_TIMESTAMP, diary.getTimestamp());
        values.put(COLUMN_PHOTO_PATH, diary.getPhotoPath());
        values.put(COLUMN_LATITUDE, diary.getLatitude());
        values.put(COLUMN_LONGITUDE, diary.getLongitude());
        values.put(COLUMN_ADDRESS, diary.getAddress());
        values.put(COLUMN_MOOD, diary.getMood());

        long id = db.insert(TABLE_NAME, null, values);
        db.close();
        return id;
    }

    // READ - Retrieve a single diary entry by ID
    public Diary getDiary(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_NAME, null, COLUMN_ID + "=?",
                new String[]{String.valueOf(id)}, null, null, null);

        Diary diary = null;
        if (cursor != null && cursor.moveToFirst()) {
            diary = cursorFromCursor(cursor);
            cursor.close();
        }
        db.close();
        return diary;
    }

    // READ - Retrieve all entries (sorted by timestamp descending, optionally filtered)
    public List<Diary> getAllDiaries(String query) {
        List<Diary> diaries = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String selectQuery;
        String[] selectionArgs = null;

        if (query == null || query.trim().isEmpty()) {
            selectQuery = "SELECT * FROM " + TABLE_NAME + " ORDER BY " + COLUMN_TIMESTAMP + " DESC";
        } else {
            selectQuery = "SELECT * FROM " + TABLE_NAME + " WHERE " 
                    + COLUMN_TITLE + " LIKE ? OR " 
                    + COLUMN_CONTENT + " LIKE ? ORDER BY " 
                    + COLUMN_TIMESTAMP + " DESC";
            selectionArgs = new String[]{"%" + query + "%", "%" + query + "%"};
        }

        Cursor cursor = db.rawQuery(selectQuery, selectionArgs);
        if (cursor.moveToFirst()) {
            do {
                diaries.add(cursorFromCursor(cursor));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return diaries;
    }

    // UPDATE - Update an existing diary entry
    public int updateDiary(Diary diary) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TITLE, diary.getTitle());
        values.put(COLUMN_CONTENT, diary.getContent());
        values.put(COLUMN_TIMESTAMP, diary.getTimestamp());
        values.put(COLUMN_PHOTO_PATH, diary.getPhotoPath());
        values.put(COLUMN_LATITUDE, diary.getLatitude());
        values.put(COLUMN_LONGITUDE, diary.getLongitude());
        values.put(COLUMN_ADDRESS, diary.getAddress());
        values.put(COLUMN_MOOD, diary.getMood());

        int count = db.update(TABLE_NAME, values, COLUMN_ID + "=?",
                new String[]{String.valueOf(diary.getId())});
        db.close();
        return count;
    }

    // DELETE - Delete a diary entry
    public int deleteDiary(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        int count = db.delete(TABLE_NAME, COLUMN_ID + "=?",
                new String[]{String.valueOf(id)});
        db.close();
        return count;
    }

    // Helper to map cursor row to Diary object
    private Diary cursorFromCursor(Cursor cursor) {
        int idIndex = cursor.getColumnIndexOrThrow(COLUMN_ID);
        int titleIndex = cursor.getColumnIndexOrThrow(COLUMN_TITLE);
        int contentIndex = cursor.getColumnIndexOrThrow(COLUMN_CONTENT);
        int timestampIndex = cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP);
        int photoPathIndex = cursor.getColumnIndexOrThrow(COLUMN_PHOTO_PATH);
        int latitudeIndex = cursor.getColumnIndexOrThrow(COLUMN_LATITUDE);
        int longitudeIndex = cursor.getColumnIndexOrThrow(COLUMN_LONGITUDE);
        int addressIndex = cursor.getColumnIndexOrThrow(COLUMN_ADDRESS);
        int moodIndex = cursor.getColumnIndexOrThrow(COLUMN_MOOD);

        return new Diary(
                cursor.getInt(idIndex),
                cursor.getString(titleIndex),
                cursor.getString(contentIndex),
                cursor.getLong(timestampIndex),
                cursor.getString(photoPathIndex),
                cursor.getDouble(latitudeIndex),
                cursor.getDouble(longitudeIndex),
                cursor.getString(addressIndex),
                cursor.getString(moodIndex)
        );
    }
}
