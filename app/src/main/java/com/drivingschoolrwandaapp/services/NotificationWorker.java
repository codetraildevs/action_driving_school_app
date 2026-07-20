package com.drivingschoolrwandaapp.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.AudioAttributes;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.hilt.work.HiltWorker;
import androidx.navigation.NavDeepLinkBuilder;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.bumptech.glide.Glide;
import com.drivingschoolrwandaapp.App;
import com.drivingschoolrwandaapp.R;
import com.drivingschoolrwandaapp.ui.activities.IremboActivity;
import com.drivingschoolrwandaapp.ui.activities.MyApplicationsActivity;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedInject;

@HiltWorker
public class NotificationWorker extends Worker {

    private static final String TAG = "NotificationWorker";

    public static final String KEY_TITLE = "key_title";
    public static final String KEY_BODY = "key_body";
    public static final String KEY_CHANNEL_ID = "key_channel_id";
    public static final String KEY_ICON_URL = "key_icon_url";

    @AssistedInject
    public NotificationWorker(
            @Assisted @NonNull Context context,
            @Assisted @NonNull WorkerParameters workerParams
    ) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "doWork: NotificationWorker has started!");

        // Create fresh channels to ensure settings are applied.
        createNotificationChannels();

        Data data = getInputData();
        String title = data.getString(KEY_TITLE);
        String body = data.getString(KEY_BODY);
        String channelId = data.getString(KEY_CHANNEL_ID);
        String largeIconUrl = data.getString(KEY_ICON_URL);

        Bitmap largeIcon = null;

        if (largeIconUrl != null && !largeIconUrl.isEmpty()) {
            try {
                largeIcon = Glide.with(getApplicationContext())
                        .asBitmap()
                        .load(largeIconUrl)
                        .submit()
                        .get(15, TimeUnit.SECONDS);
                Log.d(TAG, "doWork: Large icon downloaded successfully.");
            } catch (Exception e) {
                Log.e(TAG, "doWork: Large icon download failed (timed out or error).", e);
                com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().recordException(e);
            }
        }

        sendNotification(title, body, channelId, largeIcon);

        return Result.success();
    }

    private void sendNotification(String title, String body, String channelId, Bitmap largeIcon) {
        Context context = getApplicationContext();

        if (title == null || body == null) {
            Log.d(TAG, "Skipping notification, title or body is null");
            return;
        }

        String finalChannelId = getChannelIdFromString(channelId);

        PendingIntent pendingIntent = createPendingIntent(context, finalChannelId);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, finalChannelId)
                .setSmallIcon(R.drawable.ic_notify)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        if (largeIcon != null) {
            notificationBuilder.setLargeIcon(largeIcon);
        } else {
            // Load the default icon synchronously from local resources with downscaling.
            // BitmapFactory.decodeResource is instant for bundled drawables,
            // unlike Glide.submit().get() which adds unnecessary overhead.
            // We use inSampleSize to keep memory usage low (~128px target).
            try {
                android.graphics.BitmapFactory.Options opts = new android.graphics.BitmapFactory.Options();
                opts.inJustDecodeBounds = true;
                android.graphics.BitmapFactory.decodeResource(
                        context.getResources(), R.drawable.logo1, opts);
                
                int targetSize = 128;
                if (opts.outWidth > targetSize || opts.outHeight > targetSize) {
                    int sampleW = opts.outWidth / targetSize;
                    int sampleH = opts.outHeight / targetSize;
                    opts.inSampleSize = Math.max(sampleW, sampleH);
                } else {
                    opts.inSampleSize = 1;
                }
                opts.inJustDecodeBounds = false;
                
                Bitmap defaultLargeIcon = android.graphics.BitmapFactory.decodeResource(
                        context.getResources(), R.drawable.logo1, opts);
                if (defaultLargeIcon != null) {
                    notificationBuilder.setLargeIcon(defaultLargeIcon);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to load default notification icon", e);
            }
        }

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify((int) System.currentTimeMillis(), notificationBuilder.build());
            Log.d(TAG, "sendNotification: Notification sent to manager.");
        }
    }

    private String getChannelIdFromString(String numericId) {
        Context context = getApplicationContext();
        if (numericId == null) {
            return context.getString(R.string.general_channel_id);
        }
        switch (numericId) {
            case "1":
                return context.getString(R.string.general_channel_id);
            case "2":
                return context.getString(R.string.exams_channel_id);
            case "3":
                return context.getString(R.string.irembo_channel_id);
            case "4":
                return context.getString(R.string.application_channel_id);
            case "5":
                return context.getString(R.string.subscription_channel_id);
            default:
                return context.getString(R.string.general_channel_id);
        }
    }

    private PendingIntent createPendingIntent(Context context, String channelId) {
        String generalChannelId = context.getString(R.string.general_channel_id);
        String examsChannelId = context.getString(R.string.exams_channel_id);
        String iremboChannelId = context.getString(R.string.irembo_channel_id);
        String applicationChannelId = context.getString(R.string.application_channel_id);
        String subscriptionChannelId = context.getString(R.string.subscription_channel_id);

        if (channelId.equals(iremboChannelId)) {
            Intent intent = new Intent(context, IremboActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            return PendingIntent.getActivity(context, 1, intent,
                    PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);
        } else if (channelId.equals(applicationChannelId)) {
            Intent intent = new Intent(context, MyApplicationsActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            return PendingIntent.getActivity(context, 2, intent,
                    PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);
        } else if (channelId.equals(examsChannelId)) {
            return new NavDeepLinkBuilder(context)
                    .setComponentName(App.class)
                    .setGraph(R.navigation.main_nav_graph)
                    .setDestination(R.id.testsFragment)
                    .createPendingIntent();
        } else if (channelId.equals(subscriptionChannelId)) {
            return new NavDeepLinkBuilder(context)
                    .setComponentName(App.class)
                    .setGraph(R.navigation.main_nav_graph)
                    .setDestination(R.id.profileFragment)
                    .createPendingIntent();
        } else { // general_channel or default
            return new NavDeepLinkBuilder(context)
                    .setComponentName(App.class)
                    .setGraph(R.navigation.main_nav_graph)
                    .setDestination(R.id.dashboardFragment)
                    .createPendingIntent();
        }
    }


    private void createNotificationChannels() {
        // NotificationChannel is available from API 26+, and our minSdk is 27.
        Context context = getApplicationContext();
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) {
            return;
        }

        // General Channel
        NotificationChannel generalChannel = new NotificationChannel(
                context.getString(R.string.general_channel_id),
                context.getString(R.string.general_channel_name),
                NotificationManager.IMPORTANCE_HIGH
        );
        generalChannel.setDescription(context.getString(R.string.general_channel_description));
        generalChannel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);

        // Exams Channel with Custom Sound
        NotificationChannel examsChannel = new NotificationChannel(
                context.getString(R.string.exams_channel_id),
                context.getString(R.string.exams_channel_name),
                NotificationManager.IMPORTANCE_HIGH
        );
        examsChannel.setDescription(context.getString(R.string.exams_channel_description));
        examsChannel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);

        Uri soundUri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + context.getPackageName() + "/" + R.raw.car_horn);
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                .build();
        examsChannel.setSound(soundUri, audioAttributes);

        // Irembo Channel
        NotificationChannel iremboChannel = new NotificationChannel(
                context.getString(R.string.irembo_channel_id),
                context.getString(R.string.irembo_channel_name),
                NotificationManager.IMPORTANCE_HIGH
        );
        iremboChannel.setDescription(context.getString(R.string.irembo_channel_description));
        iremboChannel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);

        // Application Channel
        NotificationChannel applicationChannel = new NotificationChannel(
                context.getString(R.string.application_channel_id),
                context.getString(R.string.application_channel_name),
                NotificationManager.IMPORTANCE_HIGH
        );
        applicationChannel.setDescription(context.getString(R.string.application_channel_description));
        applicationChannel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);

        // Subscription Channel
        NotificationChannel subscriptionChannel = new NotificationChannel(
                context.getString(R.string.subscription_channel_id),
                context.getString(R.string.subscription_channel_name),
                NotificationManager.IMPORTANCE_HIGH
        );
        subscriptionChannel.setDescription(context.getString(R.string.subscription_channel_description));
        subscriptionChannel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);

        List<NotificationChannel> channels = Arrays.asList(generalChannel, examsChannel, iremboChannel, applicationChannel, subscriptionChannel);
        manager.createNotificationChannels(channels);
    }
}
