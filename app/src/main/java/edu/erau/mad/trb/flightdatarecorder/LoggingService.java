package edu.erau.mad.trb.flightdatarecorder;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Process;
import android.util.Log;

public class LoggingService extends Service implements Runnable {
    private final IBinder binder = new LoggingBinder();

    private HandlerThread hThread;
    private Handler handler;

    private int numPows = 0;

    public LoggingService() {}

    public class LoggingBinder extends Binder {
        LoggingService getService() {
            return LoggingService.this;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //do init stuff here
        hThread = new HandlerThread("LoggingServiceThread",
                Process.THREAD_PRIORITY_BACKGROUND);
        hThread.start();
        handler = new Handler(hThread.getLooper());
        handler.postDelayed(this, 1000);

        return Service.START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public int getNumPows() {
        return numPows;
    }

    @Override
    public void onDestroy() {
        hThread.quit();
        Log.i("LoggingService", "Bye!");
        super.onDestroy();
    }

    @Override
    public void run() {
        numPows++;
        Log.i("LoggingService", "Pow!");
        handler.postDelayed(this, 1000);
    }
}
