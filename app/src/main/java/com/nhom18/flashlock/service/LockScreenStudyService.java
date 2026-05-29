package com.nhom18.flashlock.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.nhom18.flashlock.R;
import com.nhom18.flashlock.data.model.Topic;
import com.nhom18.flashlock.data.model.UserProfile;
import com.nhom18.flashlock.data.model.Word;
import com.nhom18.flashlock.data.remote.FirebaseProfileDataSource;
import com.nhom18.flashlock.data.remote.FirebaseSavedTopicDataSource;
import com.nhom18.flashlock.data.remote.FirebaseTopicWordDataSource;
import com.nhom18.flashlock.data.repository.FirebaseProfileRepository;
import com.nhom18.flashlock.data.repository.FirebaseSavedTopicRepository;
import com.nhom18.flashlock.data.repository.FirebaseTopicWordRepository;
import com.nhom18.flashlock.data.repository.ProfileRepository;
import com.nhom18.flashlock.data.repository.SavedTopicRepository;
import com.nhom18.flashlock.data.repository.TopicWordRepository;
import com.nhom18.flashlock.receiver.LockScreenNotificationReceiver;
import com.nhom18.flashlock.ui.main.MainActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LockScreenStudyService extends Service {

    private static final String TAG = "FlashLockService";

    public static final String ACTION_REFRESH_WORDS = "com.nhom18.flashlock.ACTION_REFRESH_WORDS";
    public static final String ACTION_NEXT_WORD = "com.nhom18.flashlock.ACTION_NEXT_WORD";
    public static final String ACTION_PREV_WORD = "com.nhom18.flashlock.ACTION_PREV_WORD";

    private static final String CHANNEL_ID = "flashlock_lock_screen_v3";
    private static final int NOTIFICATION_ID = 4201;

    private BroadcastReceiver screenReceiver;
    private Handler mainHandler;

    private final List<Word> wordCache = new ArrayList<>();
    private int currentIndex = 0;
    private List<String> currentTopicIds = new ArrayList<>();
    private boolean lockScreenEnabled = true;

    @Override
    public void onCreate() {
        super.onCreate();
        mainHandler = new Handler(Looper.getMainLooper());
        createChannel();
        startForeground(NOTIFICATION_ID, buildPlaceholderNotification());
        registerScreenReceiver();
        preloadSettingsAndWords();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            String action = intent.getAction();
            Log.d(TAG, "onStartCommand action=" + action);
            switch (action) {
                case ACTION_REFRESH_WORDS:
                    preloadSettingsAndWords();
                    break;
                case ACTION_NEXT_WORD:
                    advance(+1);
                    break;
                case ACTION_PREV_WORD:
                    advance(-1);
                    break;
                default:
                    break;
            }
        }
        return START_STICKY;
    }

    private void registerScreenReceiver() {
        if (screenReceiver != null) return;
        screenReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent == null || intent.getAction() == null) return;
                if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
                    advance(+1);
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(screenReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(screenReceiver, filter);
        }
    }

    private void preloadSettingsAndWords() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Log.d(TAG, "No signed-in user, skip preload");
            return;
        }

        ProfileRepository profileRepo = new FirebaseProfileRepository(new FirebaseProfileDataSource());
        profileRepo.getCurrentUserProfile().addOnCompleteListener(task -> {
            if (!task.isSuccessful() || task.getResult() == null) return;
            UserProfile.Settings settings = task.getResult().getSettings();
            if (settings == null) return;

            lockScreenEnabled = settings.isLockScreenEnabled();
            currentTopicIds = settings.getLockScreenTopicIds() != null
                    ? settings.getLockScreenTopicIds() : new ArrayList<>();

            if (!lockScreenEnabled) {
                postNotification(buildStatusNotification(
                        getString(R.string.lock_screen_notification_disabled)));
                return;
            }
            refreshWordCache();
        });
    }

    private void refreshWordCache() {
        if (currentTopicIds != null && !currentTopicIds.isEmpty()) {
            fetchWordsForTopics(new ArrayList<>(currentTopicIds));
            return;
        }

        // Không chọn topic nào = "Tất cả": gộp My_words + mọi saved topic
        SavedTopicRepository savedTopicRepo =
                new FirebaseSavedTopicRepository(new FirebaseSavedTopicDataSource());
        savedTopicRepo.getSavedTopics().addOnCompleteListener(task -> {
            List<String> ids = new ArrayList<>();
            ids.add(Topic.MY_WORDS_TOPIC_ID);
            if (task.isSuccessful() && task.getResult() != null) {
                for (Topic t : task.getResult()) {
                    if (t.getTopicId() != null && !ids.contains(t.getTopicId())) {
                        ids.add(t.getTopicId());
                    }
                }
            }
            fetchWordsForTopics(ids);
        });
    }

    private void fetchWordsForTopics(List<String> topicIds) {
        TopicWordRepository topicWordRepo =
                new FirebaseTopicWordRepository(new FirebaseTopicWordDataSource());

        List<Task<List<Word>>> tasks = new ArrayList<>();
        for (String id : topicIds) {
            if (id != null && !id.trim().isEmpty()) {
                tasks.add(topicWordRepo.getTopicWords(id));
            }
        }
        if (tasks.isEmpty()) {
            postNotification(buildStatusNotification(
                    getString(R.string.lock_screen_notification_empty)));
            return;
        }

        Tasks.whenAllComplete(tasks).addOnCompleteListener(done -> {
            List<Word> combined = new ArrayList<>();
            for (Task<List<Word>> t : tasks) {
                if (t.isSuccessful() && t.getResult() != null) {
                    combined.addAll(t.getResult());
                }
            }
            if (combined.isEmpty()) {
                postNotification(buildStatusNotification(
                        getString(R.string.lock_screen_notification_empty)));
                return;
            }
            Collections.shuffle(combined);
            synchronized (wordCache) {
                wordCache.clear();
                wordCache.addAll(combined);
                currentIndex = 0;
            }
            postNotification(buildWordNotification(combined.get(0)));
        });
    }

    private void advance(int delta) {
        Word target = null;
        synchronized (wordCache) {
            if (wordCache.isEmpty()) {
                preloadSettingsAndWords();
                return;
            }
            int size = wordCache.size();
            currentIndex = ((currentIndex + delta) % size + size) % size;
            target = wordCache.get(currentIndex);
        }
        postNotification(buildWordNotification(target));
    }

    private void postNotification(Notification n) {
        try {
            startForeground(NOTIFICATION_ID, n);
        } catch (Exception e) {
            Log.e(TAG, "Failed to update notification: " + e.getMessage());
        }
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager == null) return;
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return;

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                getString(R.string.lock_screen_channel_name),
                NotificationManager.IMPORTANCE_LOW);
        channel.setDescription(getString(R.string.lock_screen_channel_description));
        channel.setShowBadge(false);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        channel.enableVibration(false);
        channel.setSound(null, null);
        manager.createNotificationChannel(channel);
    }

    private PendingIntent makeBroadcast(String action, int requestCode) {
        Intent intent = new Intent(this, LockScreenNotificationReceiver.class)
                .setAction(action)
                .setPackage(getPackageName());
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.getBroadcast(this, requestCode, intent, flags);
    }

    private PendingIntent makeOpenAppIntent() {
        Intent intent = new Intent(this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.getActivity(this, 100, intent, flags);
    }

    private NotificationCompat.Builder baseBuilder() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_flashlock)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .setAutoCancel(false)
                .setOnlyAlertOnce(true)
                .setSilent(true)
                .setShowWhen(false)
                .setContentIntent(makeOpenAppIntent())
                .setColor(ContextCompat.getColor(this, R.color.primary));
    }

    private Notification buildPlaceholderNotification() {
        return buildStatusNotification(getString(R.string.lock_screen_notification_text));
    }

    private Notification buildStatusNotification(String text) {
        return baseBuilder()
                .setContentTitle(getString(R.string.lock_screen_notification_title))
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .build();
    }

    private Notification buildWordNotification(Word word) {
        String term = word.getTerm() != null ? word.getTerm() : "";
        String definition = word.getDefinition() != null ? word.getDefinition() : "";

        RemoteViews collapsed = new RemoteViews(getPackageName(),
                R.layout.notification_lock_screen_word_collapsed);
        collapsed.setTextViewText(R.id.tv_term, term);
        collapsed.setTextViewText(R.id.tv_definition, definition);
        collapsed.setOnClickPendingIntent(R.id.btn_next, makeBroadcast(ACTION_NEXT_WORD, 1));

        RemoteViews expanded = new RemoteViews(getPackageName(),
                R.layout.notification_lock_screen_word);
        expanded.setTextViewText(R.id.tv_term, term);
        expanded.setTextViewText(R.id.tv_definition, definition);
        expanded.setOnClickPendingIntent(R.id.btn_next, makeBroadcast(ACTION_NEXT_WORD, 1));
        expanded.setOnClickPendingIntent(R.id.btn_prev, makeBroadcast(ACTION_PREV_WORD, 2));

        return baseBuilder()
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                .setCustomContentView(collapsed)
                .setCustomBigContentView(expanded)
                .setContentTitle(term)
                .setContentText(definition)
                .build();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Intent restart = new Intent(getApplicationContext(), this.getClass());
        restart.setPackage(getPackageName());
        try {
            ContextCompat.startForegroundService(this, restart);
        } catch (Exception e) {
            Log.w(TAG, "Restart after task removed failed: " + e.getMessage());
        }
        super.onTaskRemoved(rootIntent);
    }

    @Override
    public void onDestroy() {
        if (screenReceiver != null) {
            try {
                unregisterReceiver(screenReceiver);
            } catch (Exception ignored) {
            }
            screenReceiver = null;
        }
        if (mainHandler != null) mainHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
