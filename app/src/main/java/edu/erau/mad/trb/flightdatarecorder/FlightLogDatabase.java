package edu.erau.mad.trb.flightdatarecorder;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

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

    /** Get the only FlightLogDatabase object, or make one if it doesn't exist.
     * @param context an Android context object to associate the database with
     *                if not created yet; an application context will be
     *                obtained from this.
     * @return the only instance of a FlightLogDatabase that will exist
     */
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

    /* Called when the database is created for the first time. */
    @Override
    public void onCreate(SQLiteDatabase db) {
        //TODO remake the tables...
        /*
        final String create = "CREATE TABLE " + TABLE_FLIGHT_DATA + "(" +
                //COL_DELTAT + " INTEGER PRIMARY KEY, " +
                COL_ROLL + " REAL, " +
                COL_PITCH + " REAL, " +
                COL_LATI + " REAL, " +
                COL_LONGI + " REAL, " +
                COL_YAW + " REAL)";
        db.execSQL(create);
        */
    }

    /* Called when the database schema increases in version number */
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