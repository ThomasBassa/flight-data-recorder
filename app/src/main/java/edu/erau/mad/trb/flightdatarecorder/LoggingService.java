package edu.erau.mad.trb.flightdatarecorder;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Process;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class LoggingService extends Service implements Runnable {
    private final IBinder binder = new LoggingBinder();

    public static final int NOTIFICATION_ID = 42078502;

    private HandlerThread hThread;
    private Handler handler;

    private DevicePosAndOrient posAndOrient;
    private long sysStartTime;

    private FlightLogDatabase database;
    private SQLiteDatabase dbConnection;

    private List<LogUpdateInterface> listeners = new ArrayList<>();

    private boolean serviceStarted = false;

    public LoggingService() {}

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        serviceStarted = true;
        hThread = new HandlerThread("LoggingServiceThread",
                Process.THREAD_PRIORITY_BACKGROUND);
        hThread.start();
        handler = new Handler(hThread.getLooper());

        database = FlightLogDatabase.getInstance(this);
        final long realStartTime = System.currentTimeMillis();
        sysStartTime = SystemClock.elapsedRealtime();

        // https://developer.android.com/guide/topics/ui/notifiers/notifications.html#SimpleNotification

        NotificationCompat.Builder notBuild = new NotificationCompat.Builder(this);
        notBuild.setUsesChronometer(true)
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
        notBuild.setContentIntent(resultPendingIntent);

        startForeground(NOTIFICATION_ID, notBuild.build());

        Log.e("LoggingService", String.format("Real Start: %d == %tc",
                realStartTime, realStartTime));

        handler.post(new Runnable() {
            @Override
            public void run() {
                dbConnection = database.openLoggingDBConnection(realStartTime);
                posAndOrient = new DevicePosAndOrient(LoggingService.this);
                posAndOrient.startListening();
                handler.postDelayed(LoggingService.this, 1000);
            }
        });

        return Service.START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return serviceStarted ? binder : null;
    }

    @Override
    public void onDestroy() {
        serviceStarted = false;
        posAndOrient.stopListening();
        hThread.quit();
        final long lastDeltaT = SystemClock.elapsedRealtime() - sysStartTime;
        database.concludeLogging(dbConnection, posAndOrient, lastDeltaT);
        unregisterAllListeners();
        Log.e("LoggingService", "Service stop.");
        super.onDestroy();
    }

    @Override
    public void run() {
        final long deltaT = SystemClock.elapsedRealtime() - sysStartTime;
        publishToListeners(deltaT);
        database.logFlightData(dbConnection, posAndOrient, deltaT);
        Log.e("LoggingService", "Service tick.");
        handler.postDelayed(this, 1000);
    }

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

    public void registerListener(LogUpdateInterface listener) {
        listeners.add(listener);
    }

    public void unregisterListener(LogUpdateInterface listener) {
        listeners.remove(listener);
    }

    public void unregisterAllListeners() {
        listeners = new ArrayList<>();
    }

    /** Extremely simple Binder class that allows us to call methods on this
     * service directly, as long as we're in the same package + process. */
    public class LoggingBinder extends Binder {
        LoggingService getService() {
            return LoggingService.this;
        }
    }

    public static interface LogUpdateInterface {
        public void updateFieldData(DevicePosAndOrient data);
        public void updateClock(long deltaT);
        public void runOnUiThread(Runnable r);
    }
}
