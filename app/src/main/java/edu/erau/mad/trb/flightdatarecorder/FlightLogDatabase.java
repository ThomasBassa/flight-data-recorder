package edu.erau.mad.trb.flightdatarecorder;
/* FlightLogDatabase.java
 * SE395A Final Project
 * by Thomas Bassa
 * A Java class to handle the storage of flight data in a database. */

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDoneException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/** Database class for the storage of all flight data. Utilizes the
 * SQLiteOpenHelper to accomplish the management of the database. */
public class FlightLogDatabase extends SQLiteOpenHelper {

    /** Version of the database schema, used in onCreate/onUpgrade */
    private final static int VERSION_NO = 1;
    /** Database file name used within the Android file system (not seen much) */
    private final static String DB_NAME = "FlightLogs.db";

    /** Name of the table of orientation records. Its columns follow. */
    public final static String TABLE_FLIGHT_DATA = "FlightOrientationData";

    /** The ID of the flight, as seen in the flight list table. */
    public final static String COL_FLIGHT_ID = "flightId";

    /** CursorAdapters require a column named _id, this is a stand in for the
     * flight id. */
    public final static String COL_FLIGHT_ID_ALIAS = "_id";

    /** How many milliseconds have passed since the start of this flight */
    public final static String COL_DELTA_T_MS = "deltaTMillis";

    /** Roll of the device, in degrees from "flat," positive in right tilt
     * direction, negative in left */
    public final static String COL_ROLL = "roll";

    /** Pitch of the device, in degrees from "flat," positive down, negative up */
    public final static String COL_PITCH = "pitch";

    /** Latitude of the device, in degrees. Signed; positive N,
     * negative S of equator */
    public final static String COL_LATI = "lati";

    /** Longitude of the device, in degrees. Signed; positive E,
     * negative W of prime meridian */
    public final static String COL_LONGI = "longi";

    /** Yaw/bearing of the device, in degrees. Signed; positive clockwise from N,
     * negative counterclockwise from N */
    public final static String COL_YAW = "yaw";

    /** Altitude, height AMSL, in feet */
    public final static String COL_ALT = "altitude";


    /** The table name for the list of flights. Its columns follow, although
     * FLIGHT_ID is shared among both tables. */
    public final static String TABLE_FLIGHT_LIST = "FlightList";

    //Uses FLIGHT_ID

    /** The time this flight started, as a Unix timestamp in milliseconds */
    public final static String COL_START_REAL = "startTimeUnixMillis";

    /** The time this flight ended, as a Unix timestamp in milliseconds */
    public final static String COL_END_REAL = "endTimeUnixMillis";

    /** The saved instance of the FlightLogDatabase; used for singleton pattern */
    private static FlightLogDatabase instance;

    /** The last ID of a newly created flight, from openLoggingDBConnection */
    private long lastNewID;
    /** Whether the value of lastNewID is valid */
    private boolean lastIDvalid = false;

    /** Get the only FlightLogDatabase object, or make one if it doesn't exist.
     * @param context an Android context object to associate the database with
     *                if not created yet; an application context will be
     *                obtained from this.
     * @return the only instance of a FlightLogDatabase that will exist */
    public static FlightLogDatabase getInstance(Context context) {
        if(instance == null) {
            instance = new FlightLogDatabase(context.getApplicationContext());
        }
        return instance;
    }

    /** Create a FlightLogDatabase. Uses SQLiteOpenHelper's constructor to
     * handle the initialization. */
    private FlightLogDatabase(Context context) {
        super(context, DB_NAME, null, VERSION_NO);
        //creationContext = context;
    }

    /** Get a Cursor for all known flights in the flight list,
     * where the flights are pre-sorted in descending order. */
    public Cursor getAllFlights() {
        SQLiteDatabase db = getReadableDatabase();
        //Essentially SELECT * FROM flightList ORDER BY start DESC with some renaming
        final String query = String.format("SELECT %s AS %s, %s, %s FROM %s " +
                        "WHERE %s NOT NULL ORDER BY %s DESC",
                COL_FLIGHT_ID, COL_FLIGHT_ID_ALIAS,
                COL_START_REAL, COL_END_REAL,
                TABLE_FLIGHT_LIST,
                COL_END_REAL,
                COL_START_REAL);

        return db.rawQuery(query, null);
    }

    /** Get the real-world start time of a flight with the given id,
     * using a given database connection to perform the query.
     * @param db the database to query (from an earlier call to get*Database)
     * @param id the Flight ID to determine the start time of
     * @return a unix time, in milliseconds, representing when the flight started */
    private long getFlightStart(SQLiteDatabase db, long id) {
        final String query = String.format("SELECT %s FROM %s WHERE %s=%d",
                COL_START_REAL,
                TABLE_FLIGHT_LIST,
                COL_FLIGHT_ID, id);
        return DatabaseUtils.longForQuery(db, query, null);
    }

    /** Get a Cursor for all latitudes, longitudes, and delta times associated
     * with a given flight ID.
     * @param flightID the ID of the flight
     * @return a Cursor with the given info */
    public Cursor getFlightPositionsForFlight(long flightID) {
        SQLiteDatabase db = getReadableDatabase();

        final String query = String.format("SELECT %s, %s, %s FROM %s " +
                        "WHERE %s=%d ORDER BY %s ASC",
                COL_DELTA_T_MS, COL_LATI, COL_LONGI,
                TABLE_FLIGHT_DATA,
                COL_FLIGHT_ID, flightID,
                COL_DELTA_T_MS);
        return db.rawQuery(query, null);
    }

    /** Initiate a new flight log, and get a database object to be used with
     * future queries to {@link #logFlightData(android.database.sqlite.SQLiteDatabase, DevicePosAndOrient, long)}
     * and {@link #concludeLogging(android.database.sqlite.SQLiteDatabase, DevicePosAndOrient, long)}
     * @param realStartMillis the Unix start time, in milliseconds, of this flight
     * @return a database object for use by other FlightLogDatabase methods
     * @throws RuntimeException if called more than once in sequence without
     * calling concludeLogging afterward */
    public SQLiteDatabase openLoggingDBConnection(long realStartMillis) {
        if(lastIDvalid) throw new RuntimeException("Opened new connection " +
                "without closing the old one!");

        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues(1);
        values.put(COL_START_REAL, realStartMillis);

        /* This is a somewhat hacky multiple-return technique. Since the ID is
        intentionally not specified by anyone (SQLite generates it for us) and
        cannot also be returned to the caller, it is stored in a class field
        along with a flag to state its validity. This ID is then used in
        logFlightData and concludeLogging. */
        lastNewID = db.insert(TABLE_FLIGHT_LIST, null, values);
        lastIDvalid = true;

        return db;
    }

    /** Log a set of flight data using the provided inputs.
     * @param db a SQLiteDatabase to maintain continuity between logs
     * @param posAndOrient the position/orientation data to log
     * @param deltaTmillis the amount of time passed since the last log
     * @throws RuntimeException if called without a corresponding call to
     * openLoggingDBConnection first */
    public void logFlightData(SQLiteDatabase db, DevicePosAndOrient posAndOrient,
                              long deltaTmillis) {
        ContentValues values = new ContentValues(8);

        if(!lastIDvalid) throw new RuntimeException("Connection was not opened " +
                "with openLoggingDBConnection!");

        values.put(COL_FLIGHT_ID, lastNewID);
        values.put(COL_DELTA_T_MS, deltaTmillis);

        values.put(COL_ROLL, posAndOrient.getRoll());
        values.put(COL_PITCH, posAndOrient.getPitch());
        values.put(COL_YAW, posAndOrient.getAz());

        values.put(COL_LATI, posAndOrient.getLatitude());
        values.put(COL_LONGI, posAndOrient.getLongitude());
        values.put(COL_ALT, posAndOrient.getAltitude());

        db.insert(TABLE_FLIGHT_DATA, null, values);
    }

    /** Finalize a set of log data with the provided inputs.
     * @param db a SQLiteDatabase to maintain continuity between logs
     * @param posAndOrient the position/orientation data to log
     * @param deltaTmillis the amount of time passed since the last log
     * @throws RuntimeException if called without a corresponding call to
     * openLoggingDBConnection first */
    public void concludeLogging(SQLiteDatabase db, DevicePosAndOrient posAndOrient,
                                long deltaTmillis) {
        logFlightData(db, posAndOrient, deltaTmillis);

        /* For some reason, LoggingService is able to call this after the call
         to reset() even though it is killed as part of the delete-all sequence.
         The try/catch block catches the exception thrown on the first line in
         the event this happens; everything turns out well enough. */
        try {
            final long start = getFlightStart(db, lastNewID);
            final long end = start + deltaTmillis;
            Log.e("Database", String.format("End %d == %tc", end, end));
            ContentValues values = new ContentValues(1);
            values.put(COL_END_REAL, end);

            db.update(TABLE_FLIGHT_LIST, values, COL_FLIGHT_ID + "=" + lastNewID, null);
        } catch (SQLiteDoneException ignored) {}

        lastIDvalid = false;
        db.close();
    }

    /** Reset the database, clearing all stored data */
    public void reset() {
        onUpgrade(getWritableDatabase(), 0, 0);
    }

    /* Called when the database is created for the first time. */
    @Override
    public void onCreate(SQLiteDatabase db) {
        String create = "CREATE TABLE " + TABLE_FLIGHT_LIST + "(" +
                COL_FLIGHT_ID + " INTEGER PRIMARY KEY, " +
                COL_START_REAL + " INTEGER NOT NULL, " +
                COL_END_REAL + " INTEGER);";
        db.execSQL(create);

        create = "CREATE TABLE " + TABLE_FLIGHT_DATA + "(" +
                //Apply foreign key constraint; flight data id -> flight list id
                COL_FLIGHT_ID + " INTEGER NOT NULL REFERENCES " +
                TABLE_FLIGHT_LIST + "(" + COL_FLIGHT_ID + ")" +
                "ON UPDATE RESTRICT ON DELETE CASCADE, " +

                COL_DELTA_T_MS + " INTEGER NOT NULL, " +
                COL_ROLL + " REAL, " +
                COL_PITCH + " REAL, " +
                COL_YAW + " REAL, " +
                COL_LATI + " REAL, " +
                COL_LONGI + " REAL, " +
                COL_ALT + " REAL, " +
                //All entries should have a unique combination of ID and delta t
                "UNIQUE(" + COL_FLIGHT_ID + ", " + COL_DELTA_T_MS + "));";
        db.execSQL(create);

        /* Temporary testing inserts for new DBs
        ContentValues listRow = new ContentValues(2);
        final long now = System.currentTimeMillis();
        //From 100s ago to 25s ago, delta 75s
        listRow.put(COL_FLIGHT_ID, 1);

        listRow.put(COL_START_REAL, now - 1000000L);
        listRow.put(COL_END_REAL, now - 250000L);
        db.insert(TABLE_FLIGHT_LIST, null, listRow);
        //10s ago to 5s ago, delta 5s
        listRow.put(COL_FLIGHT_ID, 2);
        listRow.put(COL_START_REAL, now - 100000L);
        listRow.put(COL_END_REAL, now - 5000L);
        db.insert(TABLE_FLIGHT_LIST, null, listRow);

        ContentValues dataRow = new ContentValues(7);
        final java.util.Random gen = new java.util.Random();
        final double baseLati = gen.nextDouble() * 25.0;
        final double baseLongi = gen.nextDouble() * 25.0;
        dataRow.put(COL_FLIGHT_ID, 1);
        for(long deltat = 0; deltat < 70000L; deltat += 1000) {
            dataRow.put(COL_DELTA_T_MS, deltat);

            dataRow.put(COL_ROLL, gen.nextDouble() * 360.0);
            dataRow.put(COL_PITCH, gen.nextDouble() * 360.0);
            dataRow.put(COL_YAW, gen.nextDouble() * 360.0);

            dataRow.put(COL_LATI, baseLati + (deltat / 1000));
            dataRow.put(COL_LONGI, baseLongi + (deltat / 1000));
            dataRow.put(COL_ALT, gen.nextDouble() * 100.0);

            db.insert(TABLE_FLIGHT_DATA, null, dataRow);
        }

        dataRow.put(COL_FLIGHT_ID, 2);
        for(long deltat = 0; deltat < 5000L; deltat += 1000) {
            dataRow.put(COL_DELTA_T_MS, deltat);

            dataRow.put(COL_ROLL, gen.nextDouble() * 360.0);
            dataRow.put(COL_PITCH, gen.nextDouble() * 360.0);
            dataRow.put(COL_YAW, gen.nextDouble() * 360.0);

            dataRow.put(COL_LATI, baseLati - (deltat / 1000));
            dataRow.put(COL_LONGI, baseLongi - (deltat / 1000));
            dataRow.put(COL_ALT, gen.nextDouble() * 100.0);

            db.insert(TABLE_FLIGHT_DATA, null, dataRow);
        }
        //End of temporary inserts. */
    }

    /* Called when the database schema increases in version number. Currently
    just resets the entire database schema by dropping all tables and
    invoking onCreate. */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        //Nuke everything.
        String drop = "DROP TABLE " + TABLE_FLIGHT_DATA;
        db.execSQL(drop);
        drop = "DROP TABLE " + TABLE_FLIGHT_LIST;
        db.execSQL(drop);

        //And recreate the entire database
        onCreate(db);
    }
}
