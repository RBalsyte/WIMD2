package wimd;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.PointF;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import wimd.Interfaces.DatabaseInterface;


// TODO Raminta: rename
public class FingerprintDatabaseHandler extends SQLiteOpenHelper implements DatabaseInterface {

    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "jku_mapping";

    // measurements table
    private static final String TABLE_MEASUREMENTS = "measurements";
    private static final String MEASUREMENT_ID = "id";
    private static final String FINGERPRINT = "fingerprint_id";
    private static final String BSSID = "bssid";
    private static final String VALUE = "value";

    // TODO Raminta: we don't need this since we don't have any x, y positions. Delete after second review
    // fingerprint table
    private static final String TABLE_FINGERPRINTS = "fingerprints";
    private static final String FINGERPRINT_ID = "id";
    private static final String ROOM_NAME = "room_name";
    private static final String POSITION_X = "position_x";
    private static final String POSITION_Y = "position_y";

    public FingerprintDatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_MEASUREMENTS_TABLE = "CREATE TABLE " + TABLE_MEASUREMENTS + "("
                + MEASUREMENT_ID + " INTEGER PRIMARY KEY,"
                + FINGERPRINT + " INTEGER,"
                + BSSID + " TEXT,"
                + VALUE + " INTEGER" + ")";
        db.execSQL(CREATE_MEASUREMENTS_TABLE);

        String CREATE_FINGERPRINT_TABLE = "CREATE TABLE " + TABLE_FINGERPRINTS + "("
                + FINGERPRINT_ID + " INTEGER PRIMARY KEY,"
                + ROOM_NAME + " TEXT,"
                + POSITION_X + " FLOAT,"
                + POSITION_Y + " FLOAT" + ")";

        db.execSQL(CREATE_FINGERPRINT_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        // Drop tables if existed
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MEASUREMENTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_FINGERPRINTS);

        // Create tables again
        onCreate(db);
    }

    @Override
    //TODO Raminta: look through, adjust and document
    public void addFingerprint(Fingerprint fingerprint) {
        SQLiteDatabase db = this.getWritableDatabase();
        PointF location = fingerprint.getLocation();

        ContentValues fingerprintValues = new ContentValues();
        fingerprintValues.put(ROOM_NAME, fingerprint.getRoom());
        fingerprintValues.put(POSITION_X, location.x);
        fingerprintValues.put(POSITION_Y, location.y);

        long fingerprintID = db.insert(TABLE_FINGERPRINTS, null, fingerprintValues);

        if (fingerprintID != -1){
            Map<String, Integer> m = fingerprint.getMeasurements();
            for (String key : m.keySet()){
                int value = m.get(key);

                ContentValues mValues = new ContentValues();
                mValues.put(FINGERPRINT, fingerprintID);
                mValues.put(BSSID, key);
                mValues.put(VALUE, value);
                db.insert(TABLE_MEASUREMENTS, null, mValues);
            }
        }

        db.close();
    }

    @Override
    //TODO Raminta: look through, adjust and document
    public Fingerprint getFingerprint(int id) {
        Fingerprint fingerprint = null;

        SQLiteDatabase db = this.getReadableDatabase();

        // SQL query
        Cursor cursor = db.query(TABLE_FINGERPRINTS,
                new String[] {FINGERPRINT_ID, ROOM_NAME, POSITION_X, POSITION_Y},
                FINGERPRINT_ID + " = ?", new String[] { String.valueOf(id) },
                null, null, null, null);

        if (cursor.moveToFirst()) {
            // parse fingerprint data
            String map = cursor.getString(1);
            PointF location = new PointF(cursor.getFloat(2), cursor.getFloat(3));
            Map<String, Integer> measurements = getMeasurements(id);

            // create fingerprint
            fingerprint = new Fingerprint(id, map, location, measurements);
        }

        cursor.close();
        db.close();
        return fingerprint;
    }
    //TODO Raminta: look through, adjust and document
    public ArrayList<Fingerprint> getAllFingerprints() {
        ArrayList<Fingerprint> fingerprints = new ArrayList<>();

        String SELECT_QUERY = "SELECT * FROM " + TABLE_FINGERPRINTS;
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(SELECT_QUERY, null); // SQL query

        // loop through all fingerprint rows and add to list
        if (cursor.moveToFirst()) {
            do {
                // parse fingerprint data
                int id = cursor.getInt(0);
                String room = cursor.getString(1);
                PointF location = new PointF(cursor.getFloat(2), cursor.getFloat(3));

                Map<String, Integer> measurements = getMeasurements(id); // query measurements

                Fingerprint fingerprint = new Fingerprint(id, room, location, measurements); // create fingerprint

                fingerprints.add(fingerprint); // add to list returned fingerprints
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return fingerprints;
    }
    //TODO Raminta: look through, adjust and document
    public Map<String, Integer> getMeasurements(int fingerprintId) {
        SQLiteDatabase db = this.getReadableDatabase();

        // SQL query
        Cursor cursor = db.query(TABLE_MEASUREMENTS,
                new String[] {BSSID, VALUE},
                FINGERPRINT + " = ?", new String[] { String.valueOf(fingerprintId) },
                null, null, null, null);

        Map<String, Integer> measurements = new HashMap<String, Integer>();

        // loop through all measurement rows and add to list
        if(cursor.moveToFirst()) {
            do {
                // parse measurement data
                String BSSID = cursor.getString(0);
                int level = cursor.getInt(1);

                measurements.put(BSSID, level); // add to list
            } while(cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return measurements;
    }
    //TODO Raminta: look through, adjust and document
    public int getFingerprintCount() {
        String COUNT_QUERY = "SELECT  * FROM " + TABLE_FINGERPRINTS;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(COUNT_QUERY, null); // SQL query
        int count = cursor.getCount();

        cursor.close();
        db.close();
        return count;
    }
    //TODO Raminta: look through, adjust and document
    public void deleteFingerprint(Fingerprint fingerprint) {
        SQLiteDatabase db = this.getWritableDatabase();

        // SQL query for deleting fingerprint
        db.delete(TABLE_FINGERPRINTS, FINGERPRINT_ID + " = ?",
                new String[] { String.valueOf(fingerprint.getId()) });

        // SQL query for deleting measurements linked to given fingerprint
        db.delete(TABLE_MEASUREMENTS, FINGERPRINT + " = ?",
                new String[] { String.valueOf(fingerprint.getId()) });

        db.close();
    }
    //TODO Raminta: look through, adjust and document
    public void deleteAllFingerprints() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_FINGERPRINTS, null, null); // delete all fingerprints
        db.delete(TABLE_MEASUREMENTS, null, null); // delete all measurements
        db.close();
    }


}