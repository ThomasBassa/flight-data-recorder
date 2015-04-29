package edu.erau.mad.trb.flightdatarecorder;
/* LoggingService.java
 * SE395A Final Project
 * by Thomas Bassa
 * A Java class to handle the background logging of flight data. */

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;

import java.util.ArrayList;
import java.util.List;

/** This Service handles the recording of sensor data in the background,
 * so that the device may be used for other tasks while continuing to log data. */
public class LoggingService extends Service implements Runnable {
    /** A copy of our local IBinder implementation */
    private final IBinder binder = new LoggingBinder();

    /** ID used to build notifications. Selected through use of random.org */
    public static final int NOTIFICATION_ID = 42078502;

    /** Thread used to handle periodic generation of activity */
    private HandlerThread hThread;
    /** Handler used to enqueue tasks to hThread */
    private Handler handler;

    /** Object to manage the tracking of position and orientation in space */
    private DevicePosAndOrient posAndOrient;

    /** The system uptime at which logging was initiated */
    private long sysStartTime;

    /** Database management object */
    private FlightLogDatabase database;
    /** Database connection, used in tandem with the FlightLogDatabase */
    private SQLiteDatabase dbConnection;

    /** List of objects that need UI updates */
    private List<LogUpdateInterface> listeners = new ArrayList<>();

    /** Flag to ensure service was started via startService and not
     * bindService-- both of these could start the service,
     * but binding is useless if the service is not started first. */
    private boolean serviceStarted = false;

    /** Empty constructor; the initialization mainly takes place in onStartCommand */
    public LoggingService() {}

    //Invoked when a Context (activity) calls startService
    //This is used to initiate logging.
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //Set our flag that we started from the correct entry point
        serviceStarted = true;

        //Initiate our handler thread and handler
        hThread = new HandlerThread("LoggingServiceThread");
        hThread.start();
        handler = new Handler(hThread.getLooper());

        //Initiate the database if needed
        database = FlightLogDatabase.getInstance(this);

        //Build a notification to make this activity a foreground activity
        //Most of this code is copied + edited from
        // https://developer.android.com/guide/topics/ui/notifiers/notifications.html#SimpleNotification

        NotificationCompat.Builder notiBuilder = new NotificationCompat.Builder(this);
        notiBuilder.setUsesChronometer(true)
                .setSmallIcon(R.drawable.ic_action_locate)
                .setContentText(getText(R.string.loggingInProgress))
                .setContentTitle(getText(R.string.loggingNotificationTitle));

        // Creates an explicit intent for an Activity in your app
        Intent resultIntent = new Intent(this, MainLoggingActivity.class);

        // The stack builder object will contain an artificial back stack for the
        // started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(MainLoggingActivity.class);
        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        notiBuilder.setContentIntent(resultPendingIntent);

        //With our notification ready, become a foreground service
        startForeground(NOTIFICATION_ID, notiBuilder.build());

        //Get the time that logging began
        final long realStartTime = System.currentTimeMillis();
        sysStartTime = SystemClock.elapsedRealtime();

        //Initiate activity on our independent thread
        handler.post(new Runnable() {
            @Override
            public void run() {
                //Open the database connection and kick posAndOrient into gear
                dbConnection = database.openLoggingDBConnection(realStartTime);
                posAndOrient = new DevicePosAndOrient(LoggingService.this);
                posAndOrient.startListening();
                //Calls LoggingService.run() 1s from now on this thread
                handler.postDelayed(LoggingService.this, 1000);
            }
        });

        //Reboot the service if killed.
        return Service.START_STICKY;
    }

    //Called when a Context calls bindService. Returns a LoggingBinder object.
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    //Called when stopService is called (and no contexts bound; should be ensured)
    @Override
    public void onDestroy() {
        //Assuming that the service was started normally...
        if(serviceStarted) {
            //Reset the started flag (just in case)
            serviceStarted = false;
            //Turn off position/location updates
            posAndOrient.stopListening();
            //Kill the HandlerThread
            hThread.quit();

            //Finish the database recording.
            final long lastDeltaT = SystemClock.elapsedRealtime() - sysStartTime;
            database.concludeLogging(dbConnection, posAndOrient, lastDeltaT);

            //Unregister UI listeners...
            unregisterAllListeners();
        }
        super.onDestroy();
    }

    //This run method is repeatedly called by the Handler/HandlerThread every second.
    @Override
    public void run() {
        //Calculate time since we began
        final long deltaT = SystemClock.elapsedRealtime() - sysStartTime;

        //Update the UI of any available listeners
        publishToListeners(deltaT);

        //Update the database
        database.logFlightData(dbConnection, posAndOrient, deltaT);

        //Call this again in one second
        handler.postDelayed(this, 1000);
    }

    /** Publish time and position/orient. updates to any subscribed activities */
    private void publishToListeners(final long deltaT) {
        for(final LogUpdateInterface listener : listeners) {
            listener.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    listener.updateClock(deltaT);
                    listener.updateFieldData(posAndOrient);
                }
            });
        }
    }

    /** Add a listener to receive clock and position/orientation updates */
    public void registerListener(LogUpdateInterface listener) {
        listeners.add(listener);
    }

    /** Remove a listener; stop getting updates */
    public void unregisterListener(LogUpdateInterface listener) {
        listeners.remove(listener);
    }

    /** Remove all listeners; don't publish updates to anyone */
    public void unregisterAllListeners() {
        listeners = new ArrayList<>();
    }

    /** Extremely simple Binder class that allows us to call methods on this
     * service directly, as long as we're in the same package + process. */
    public class LoggingBinder extends Binder {
        /** Get a LoggingService directly, but only if it was started properly.
         * @return a LoggingService if initiated with startService, otherwise null */
        LoggingService getService() {
            return serviceStarted ? LoggingService.this : null;
        }
    }

    /** An interface to have a LoggingService communicate with an Activity or
     * other subscribed object. These methods are called on registered
     * listeners periodically. */
    public static interface LogUpdateInterface {

        /** Update position and orientation data */
        public void updateFieldData(DevicePosAndOrient data);
        /** Update clock data */
        public void updateClock(long deltaT);

        /** Run a method on the UI thread (as the other methods should involve
         * changing Views (This is implemented for subclasses of Activity.) */
        public void runOnUiThread(Runnable r);
    }
}
