package hk.ust.cse.comp4521.mapspoialert.provider;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import java.util.Arrays;
import java.util.HashSet;

import static hk.ust.cse.comp4521.mapspoialert.provider.POIContract.*;

public class POIDbProvider extends ContentProvider {

    private static final String TAG = "POIDbAdapter";
    private DatabaseHelper mDbHelper;

    /**
     * Database creation sql statement
     *  Each row contains ID, place Name, Latitude and Longitude
     */
    private static final String SQL_CREATE =
            "create table " +
                    POIEntry.DATABASE_TABLE_NAME + " (" +
                    POIEntry._ID + " integer primary key autoincrement, " +
                    POIEntry.COLUMN_POI + " text not null," +
                    POIEntry.COLUMN_LATITUDE + " double," +
                    POIEntry.COLUMN_LONGITUDE + " double);";

    private static final String SQL_DELETE =
            "DROP TABLE IF EXISTS " + POIEntry.DATABASE_TABLE_NAME ;

    // Used for the UriMacher
    private static final int POIALL = 10;
    private static final int POI_ID = 20;

    public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE
            + "/poiall";
    public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE
            + "/poiid";

    private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        sURIMatcher.addURI(POIProvider.AUTHORITY, POIProvider.BASE_PATH, POIALL);
        sURIMatcher.addURI(POIProvider.AUTHORITY, POIProvider.BASE_PATH + "/#", POI_ID);
    }

    // This is the database SQLite Open helper. This provides the methods to create, open and update the database
    private static class DatabaseHelper extends SQLiteOpenHelper {

        private static final String DATABASE_NAME = "POIdata";
        private static final int DATABASE_VERSION = 1;

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        // Create the SQLite database if it does not exist
        @Override
        public void onCreate(SQLiteDatabase db) {

            // Execute the SQL statement to create the database and the table
            db.execSQL(SQL_CREATE);

            // Insert a few rows into the database for a few places. You can add more if you wish
            // I get the latitude and longitude coordinates for the place using Google Maps in a browser
            insertplaceInfo(db, "Central MTR", 22.28211, 114.1578);
            insertplaceInfo(db, "Hang Hau MTR", 22.320031, 114.268812);
            insertplaceInfo(db, "Choi Hung MTR", 22.334883, 114.209063);
            insertplaceInfo(db, "HKUST Piazza", 22.337504, 114.262985);

        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                    + newVersion + ", which will destroy all old data");
            db.execSQL(SQL_DELETE);
            onCreate(db);
        }

        // Inserts a row into the database
        private void insertplaceInfo(SQLiteDatabase db, String place, double latitude, double longitude) {
            ContentValues initialValues = new ContentValues();
            initialValues.put(POIEntry.COLUMN_POI, place);
            initialValues.put(POIEntry.COLUMN_LATITUDE, latitude);
            initialValues.put(POIEntry.COLUMN_LONGITUDE, longitude);

            db.insert(POIEntry.DATABASE_TABLE_NAME, null, initialValues);
        }
    }


    public boolean onCreate() {
        mDbHelper = new DatabaseHelper(getContext());

        return false;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    // Create a new row in the database corresponding to a new location

    @Override
    public Uri insert(Uri uri, ContentValues values) {

        int uriType = sURIMatcher.match(uri);
        SQLiteDatabase sqlDB = mDbHelper.getWritableDatabase();
        long id = 0;

        switch (uriType) {
            case POIALL:
                id = sqlDB.insert(POIEntry.DATABASE_TABLE_NAME, null, values);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);

        return Uri.parse(POIProvider.BASE_PATH + "/" + id);

    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {

        int uriType = sURIMatcher.match(uri);
        SQLiteDatabase sqlDB = mDbHelper.getWritableDatabase();

        int rowsDeleted = 0;

        switch (uriType) {
            case POIALL:
                rowsDeleted = sqlDB.delete(POIEntry.DATABASE_TABLE_NAME, selection,
                        selectionArgs);
                break;

            case POI_ID:

                String id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    rowsDeleted = sqlDB.delete(POIEntry.DATABASE_TABLE_NAME,
                            POIEntry._ID + "=" + id,
                            null);
                } else {
                    rowsDeleted = sqlDB.delete(POIEntry.DATABASE_TABLE_NAME,
                            POIEntry._ID + "=" + id
                                    + " and " + selection,
                            selectionArgs);
                }
                break;

            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return rowsDeleted;

    }

    /**
     * Return a Cursor positioned at the note that matches the given rowId
     *
     * @return Cursor positioned to matching place info, if found
     * @throws SQLException if note could not be found/retrieved
     */

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {

        // Using SQLiteQueryBuilder instead of query() method
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();

        // Check if the caller has requested a column which does not exists
        checkColumns(projection);

        // Set the table
        queryBuilder.setTables(POIEntry.DATABASE_TABLE_NAME);

        int uriType = sURIMatcher.match(uri);
        switch (uriType) {
            case POIALL:
                break;
            case POI_ID:
                // Adding the ID to the original query
                queryBuilder.appendWhere(POIEntry._ID + "="
                        + uri.getLastPathSegment());
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        Cursor cursor = queryBuilder.query(db, projection, selection,
                selectionArgs, null, null, sortOrder);
        // Make sure that potential listeners are getting notified
        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        return cursor;

    }

    /**
     * Update the place info using the details provided. The place info to be updated is
     * specified using the rowId, and it is altered to use the place name, latitude, and longitude
     * values passed in
     *
     * @return true if the note was successfully updated, false otherwise
     */

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {

        int uriType = sURIMatcher.match(uri);
        SQLiteDatabase sqlDB = mDbHelper.getWritableDatabase();

        int rowsUpdated = 0;

        switch (uriType) {
            case POIALL:
                rowsUpdated = sqlDB.update(POIEntry.DATABASE_TABLE_NAME,
                        values,
                        selection,
                        selectionArgs);
                break;
            case POI_ID:
                String id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    rowsUpdated = sqlDB.update(POIEntry.DATABASE_TABLE_NAME,
                            values,
                            POIEntry._ID + "=" + id,
                            null);
                } else {
                    rowsUpdated = sqlDB.update(POIEntry.DATABASE_TABLE_NAME,
                            values,
                            POIEntry._ID + "=" + id
                                    + " and "
                                    + selection,
                            selectionArgs);
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);

        return rowsUpdated;
    }

    private void checkColumns(String[] projection) {
        String[] available = { POIEntry.COLUMN_POI,
                POIEntry.COLUMN_LATITUDE, POIEntry.COLUMN_LONGITUDE,
                POIEntry._ID };

        if (projection != null) {
            HashSet<String> requestedColumns = new HashSet<String>(Arrays.asList(projection));
            HashSet<String> availableColumns = new HashSet<String>(Arrays.asList(available));
            // Check if all columns which are requested are available
            if (!availableColumns.containsAll(requestedColumns)) {
                throw new IllegalArgumentException("Unknown columns in projection");
            }
        }
    }

}
