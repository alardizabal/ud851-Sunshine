package com.albertlardizabal.wearable;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.WindowInsets;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.Wearable;

import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Created by albertlardizabal on 1/29/17.
 */

public class WatchFaceService extends CanvasWatchFaceService {

    private static final String TAG = "WatchFaceService";

    private static final Typeface BOLD_TYPEFACE =
            Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD);
    private static final Typeface NORMAL_TYPEFACE =
            Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);

    private static final int MSG_UPDATE_TIME_ID = 42;

    /**
     * Update rate in milliseconds for normal (not ambient and not mute) mode. We update twice
     * a second to blink the colons.
     */
    private static final long NORMAL_UPDATE_RATE_MS = 1000;

    /**
     * Update rate in milliseconds for mute mode. We update every minute, like in ambient mode.
     */
    private static final long MUTE_UPDATE_RATE_MS = TimeUnit.MINUTES.toMillis(1);

    public static GoogleApiClient googleApiClient;

    private Time displayTime;

    private Paint backgroundColorPaint;
    private Paint textColorPaint;

    private boolean hasTimeZoneReceiverBeenRegistered = false;
    private boolean isInMuteMode;
    private boolean isLowBitAmbient;

    private float xOffset;
    private float yOffset;

    private int backgroundColor = Color.parseColor( "black" );
    private int textColor = Color.parseColor( "red" );

    private String highTemp;
    private String lowTemp;

    Paint backgroundPaint;
    Paint hourPaint;
    Paint minutePaint;
    Paint colonPaint;
    float colonWidth;

    Calendar calendar;
    Date date;
    SimpleDateFormat dayOfWeekFormat;
    java.text.DateFormat dateFormat;

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

        static final String COLON_STRING = ":";

        static final int MSG_UPDATE_TIME = 0;

        /** How often {@link #updateTimeHandler} ticks in milliseconds. */
        long interactiveUpdateRateMs = NORMAL_UPDATE_RATE_MS;

        /** Handler to update the time periodically in interactive mode. */
        final Handler updateTimeHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case MSG_UPDATE_TIME:
                        if (Log.isLoggable(TAG, Log.VERBOSE)) {
                            Log.v(TAG, "updating time");
                        }
                        invalidate();
                        if (shouldTimerBeRunning()) {
                            long timeMs = System.currentTimeMillis();
                            long delayMs =
                                    interactiveUpdateRateMs - (timeMs % interactiveUpdateRateMs);
                            updateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
                        }
                        break;
                }
            }
        };

        /**
         * Handles time zone and locale changes.
         */
        final BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                calendar.setTimeZone(TimeZone.getDefault());
                initFormats();
                invalidate();
            }
        };

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);
            /* initialize your watch face */
            System.out.println("Watch face running");

            setWatchFaceStyle(new WatchFaceStyle.Builder(WatchFaceService.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_VARIABLE)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .build());

            Resources resources = WatchFaceService.this.getResources();
            xOffset = resources.getDimension(R.dimen.digital_x_offset);
            yOffset = resources.getDimension(R.dimen.digital_y_offset);

            googleApiClient = new GoogleApiClient.Builder(WatchFaceService.this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(Wearable.API)
                    .build();

            // Colors
            backgroundPaint = new Paint();
            backgroundPaint.setColor(getResources().getColor(R.color.blue));
            hourPaint = createTextPaint(getResources().getColor(R.color.white), BOLD_TYPEFACE);
            minutePaint = createTextPaint(getResources().getColor(R.color.white), BOLD_TYPEFACE);
            colonPaint = createTextPaint(getResources().getColor(R.color.white));

            // Initialize member variables
            calendar = Calendar.getInstance();
            date = new Date();
            initFormats();
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onApplyWindowInsets: " + (insets.isRound() ? "round" : "square"));
            }
            super.onApplyWindowInsets(insets);

            Resources resources = WatchFaceService.this.getResources();
            boolean isRound = insets.isRound();

            float textSize = resources.getDimension(isRound
                    ? R.dimen.digital_text_size_round : R.dimen.digital_text_size);

            hourPaint.setTextSize(textSize);
            minutePaint.setTextSize(textSize);
            colonPaint.setTextSize(textSize);

            colonWidth = colonPaint.measureText(COLON_STRING);
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            /* draw your watch face */

            // Draw the background
            canvas.drawRect(0, 0, bounds.width(), bounds.height(), backgroundPaint);

            long now = System.currentTimeMillis();
            calendar.setTimeInMillis(now);
            date.setTime(now);

            // Draw the hours.
            float x = xOffset;
            String hourString;
            int hour = calendar.get(Calendar.HOUR_OF_DAY);
            hourString = String.valueOf(hour);
            canvas.drawText(hourString, x, yOffset, hourPaint);

            x += hourPaint.measureText(hourString);

            canvas.drawText(COLON_STRING, x, yOffset, colonPaint);

            x += colonWidth;

            // Draw minutes
            String minuteString = formatTwoDigitNumber(calendar.get(Calendar.MINUTE));
            canvas.drawText(minuteString, x, yOffset, minutePaint);
            x += minutePaint.measureText(minuteString);
        }

        @Override
        public void onDestroy() {
            updateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
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
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);
            /* the watch face became visible or invisible */
        }

        @Override // GoogleApiClient.ConnectionCallbacks
        public void onConnected(Bundle connectionHint) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onConnected al al al: " + connectionHint);
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


        // Initialization
        private void initFormats() {
            dayOfWeekFormat = new SimpleDateFormat("EEEE", Locale.getDefault());
            dayOfWeekFormat.setCalendar(calendar);
            dateFormat = DateFormat.getDateFormat(WatchFaceService.this);
            dateFormat.setCalendar(calendar);
        }

        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }


        // Color helpers
        private Paint createTextPaint(int defaultInteractiveColor) {
            return createTextPaint(defaultInteractiveColor, NORMAL_TYPEFACE);
        }

        private Paint createTextPaint(int defaultInteractiveColor, Typeface typeface) {
            Paint paint = new Paint();
            paint.setColor(defaultInteractiveColor);
            paint.setTypeface(typeface);
            paint.setAntiAlias(true);
            return paint;
        }


        // Time helpers
        private String formatTwoDigitNumber(int hour) {
            return String.format("%02d", hour);
        }
    }
}
