package com.albertlardizabal.wearable.providers;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.util.Log;
import android.view.SurfaceHolder;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEventBuffer;

import java.util.concurrent.TimeUnit;

/**
 * Created by albertlardizabal on 1/26/17.
 */

public class WatchFaceServiceA extends CanvasWatchFaceService {

    private static final String TAG = "WatchFaceServiceA";

    private static final Typeface NORMAL_TYPEFACE =
            Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);

    /**
     * Update rate in milliseconds for normal (not ambient and not mute) mode. We update twice
     * a second to blink the colons.
     */
    private static final long NORMAL_UPDATE_RATE_MS = 500;

    /**
     * Update rate in milliseconds for mute mode. We update every minute, like in ambient mode.
     */
    private static final long MUTE_UPDATE_RATE_MS = TimeUnit.MINUTES.toMillis(1);

    private String highTemp;
    private String lowTemp;

    @Override
    public Engine onCreateEngine() {
        /* provide your watch face implementation */
        return new Engine();
    }

    /* implement service callback methods */
    private class Engine extends CanvasWatchFaceService.Engine implements
            DataApi.DataListener,
            GoogleApiClient.ConnectionCallbacks,
            GoogleApiClient.OnConnectionFailedListener {

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);
            /* initialize your watch face */
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            /* get device features (burn-in, low-bit ambient) */
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            /* the time changed */
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            /* the wearable switched between modes */
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            /* draw your watch face */
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);
            /* the watch face became visible or invisible */
        }

        @Override // GoogleApiClient.ConnectionCallbacks
        public void onConnected(Bundle connectionHint) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onConnected: " + connectionHint);
            }
            getWeatherData();
        }

        public void getWeatherData() {
//            PutDataMapRequest putDataMapRequest = PutDataMapRequest.create("/weather");
//            putDataMapRequest.getDataMap().putString("weatherDate", mForecastAdapter.dateString);
//            putDataMapRequest.getDataMap().putString("highTemp", mForecastAdapter.highString);
//            putDataMapRequest.getDataMap().putString("lowTemp", mForecastAdapter.lowString);
//            putDataMapRequest.getDataMap().putInt("weatherImageId", mForecastAdapter.watchWeatherImageId);

//            System.out.println(putDataMapRequest.getDataMap().getString("weatherDate"));
//            System.out.println(putDataMapRequest.getDataMap().getString("highTemp"));
//            System.out.println(putDataMapRequest.getDataMap().getString("lowTemp"));
//            System.out.println(putDataMapRequest.getDataMap().getInt("weatherImageId"));
        }

        @Override // GoogleApiClient.ConnectionCallbacks
        public void onConnectionSuspended(int cause) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onConnectionSuspended: " + cause);
            }
        }

        @Override // GoogleApiClient.OnConnectionFailedListener
        public void onConnectionFailed(ConnectionResult result) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onConnectionFailed: " + result);
            }
        }

        @Override
        public void onDataChanged(DataEventBuffer dataEventBuffer) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onDataChanged");
            }
        }
    }
}
