package com.example.kienphung.lesson5;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.RequiresApi;
import android.view.View;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MusicPlayerService extends Service
        implements MediaPlayer.OnPreparedListener, MediaPlayer.OnSeekCompleteListener
        , MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener, View.OnClickListener {
    private static final String RESOURCE_PATH =
            "android.resource://com.example.kienphung.lesson5/";
    private static final String ACTION_PREVIOUS =
            "com.example..kienphung.lesson5.ACTION_PREVIOUS";
    private static final String ACTION_PLAY =
            "com.example..kienphung.lesson5.ACTION_PLAY";
    private static final String ACTION_PAUSE =
            "com.example..kienphung.lesson5.ACTION_PAUSE";
    private static final String ACTION_STOP =
            "com.example..kienphung.lesson5.ACTION_STOP";
    private static final String ACTION_NEXT =
            "com.example..kienphung.lesson5.ACTION_NEXT";
    private static final int NOTIFICATION_ID = 1000;
    private static final int CODE_PLAY = 1001;
    private static final int CODE_PAUSE = 1002;
    private static final int CODE_NEXT = 1003;
    private static final int CODE_PREVIOUS = 1004;
    private final IBinder mBinder = new LocalBinder();
    private MediaPlayer mMediaPlayer;
    private List<Song> mListSongs;
    private int mCurrentSongIndex;
    private int mPausedPosition;
    private boolean mIsPlaying;
    private boolean mOldState, mIsSkipped;
    private OnPlaybackStatusChangeListener mOnPlaybackStatusChangeListener;

    public MusicPlayerService() {
    }

    public void setOnPlaybackStatusChangeListener(OnPlaybackStatusChangeListener onPlaybackStatusChangeListener) {
        this.mOnPlaybackStatusChangeListener = onPlaybackStatusChangeListener;
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Override
    public void onCreate() {
        loadAudioFromRaw();
        initMediaPlayer();
        mCurrentSongIndex = 0;
        playSong();
        super.onCreate();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        handleIncomingActions(intent);
        return START_NOT_STICKY;
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mMediaPlayer != null) {
            stopMusic();
            mMediaPlayer.release();
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        startMusic();
    }

    @Override
    public void onSeekComplete(MediaPlayer mediaPlayer) {
        mOnPlaybackStatusChangeListener.updateCurrentTime(mMediaPlayer.getCurrentPosition());
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        nextSong();
    }

    @Override
    public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
        return false;
    }

    private void initMediaPlayer() {
        if (mMediaPlayer == null) {
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setOnPreparedListener(this);
            mMediaPlayer.setOnSeekCompleteListener(this);
            mMediaPlayer.setOnCompletionListener(this);
            mMediaPlayer.setOnErrorListener(this);
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        }

    }

    private void playSong() {
        mMediaPlayer.reset();
        mIsPlaying = false;
        try {
            mMediaPlayer.setDataSource(this, mListSongs.get(mCurrentSongIndex).getUri());
        } catch (IOException e) {
            e.printStackTrace();
        }
        mMediaPlayer.prepareAsync();
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    private void startMusic() {
        if (!mMediaPlayer.isPlaying()) {
            mMediaPlayer.start();
            mIsPlaying = true;
            startForeground(NOTIFICATION_ID, buildNotification());
            if (mOnPlaybackStatusChangeListener != null) {
                mOnPlaybackStatusChangeListener.playbackStatusChange(mIsPlaying);
                mOnPlaybackStatusChangeListener.updateCurrentTime(0);
                mOnPlaybackStatusChangeListener.updateSongImageAndDuration(mListSongs.get(mCurrentSongIndex).getPicture(), mMediaPlayer.getDuration());
            }
        }
        if (mIsSkipped && !mOldState) {
            pauseMusic();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    private void pauseMusic() {
        if (mMediaPlayer.isPlaying()) {
            mMediaPlayer.pause();
            mIsPlaying = false;
            mPausedPosition = mMediaPlayer.getCurrentPosition();
            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE))
                    .notify(NOTIFICATION_ID, buildNotification());
            stopForeground(false);
            if (mOnPlaybackStatusChangeListener != null) {
                mOnPlaybackStatusChangeListener.playbackStatusChange(mIsPlaying);
            }
        }

    }

    public void seekTo(int position) {
        mMediaPlayer.seekTo(position);
        if (!mIsPlaying) {
            mPausedPosition = position;
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    private void resumeMusic() {
        if (!mMediaPlayer.isPlaying()) {
            mMediaPlayer.seekTo(mPausedPosition);
            mMediaPlayer.start();
            mIsPlaying = true;
            startForeground(NOTIFICATION_ID, buildNotification());
            if (mOnPlaybackStatusChangeListener != null) {
                mOnPlaybackStatusChangeListener.playbackStatusChange(mIsPlaying);
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    private void stopMusic() {
        if (mMediaPlayer == null) return;
        if (mMediaPlayer.isPlaying()) {
            mMediaPlayer.stop();
            mIsPlaying = false;
            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE))
                    .notify(NOTIFICATION_ID, buildNotification());
            stopForeground(false);
            if (mOnPlaybackStatusChangeListener != null) {
                mOnPlaybackStatusChangeListener.playbackStatusChange(mIsPlaying);
            }
        }
    }

    private void nextSong() {
        mCurrentSongIndex = mCurrentSongIndex == mListSongs.size() - 1 ? 0 : ++mCurrentSongIndex;
        mOldState = mIsPlaying;
        mIsSkipped = true;
        playSong();
    }

    private void previousSong() {
        mCurrentSongIndex = mCurrentSongIndex == 0 ? mListSongs.size() - 1 : --mCurrentSongIndex;
        mOldState = mIsPlaying;
        mIsSkipped = true;
        playSong();
    }

    private void loadAudioFromRaw() {
        mListSongs = new ArrayList<>();
        int[] songIds = {R.raw.thay_the, R.raw.em_chang_sao_ma,
                R.raw.thay_the, R.raw.em_chang_sao_ma};
        for (int songId : songIds) {
            Uri uri = Uri.parse(RESOURCE_PATH + songId);
            MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
            mediaMetadataRetriever.setDataSource(this, uri);
            String title = mediaMetadataRetriever
                    .extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
            String artist = mediaMetadataRetriever
                    .extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
            byte[] picture = mediaMetadataRetriever.getEmbeddedPicture();
            mListSongs.add(new Song(uri, title, artist, picture));
            mediaMetadataRetriever.release();
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    private Notification buildNotification() {
        int actionPlayPauseImageId;
        PendingIntent playPauseAction;
        if (mIsPlaying) {
            actionPlayPauseImageId = R.drawable.ic_pause_black_24dp;
            playPauseAction = getPlayBackAction(CODE_PAUSE);
        } else {
            actionPlayPauseImageId = R.drawable.ic_play_black_24dp;
            playPauseAction = getPlayBackAction(CODE_PLAY);
        }
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(this, 0, intent
                        , PendingIntent.FLAG_UPDATE_CURRENT);
        Bitmap picture = BitmapFactory
                .decodeByteArray(mListSongs.get(mCurrentSongIndex).getPicture()
                        , 0, mListSongs.get(mCurrentSongIndex).getPicture().length);
        Notification.Builder builder = new Notification.Builder(this)
                .setShowWhen(false)
                .setStyle(new Notification.MediaStyle())
                .setSmallIcon(android.R.drawable.sym_def_app_icon)
                .setLargeIcon(picture)
                .setContentTitle(mListSongs.get(mCurrentSongIndex).getTitle())
                .setContentText(mListSongs.get(mCurrentSongIndex).getArtist())
                .setContentIntent(pendingIntent)
                .addAction(R.drawable.ic_skip_previous_black_24dp
                        , getString(R.string.action_previous), getPlayBackAction(CODE_PREVIOUS))
                .addAction(actionPlayPauseImageId
                        , getString(R.string.action_play_pause), playPauseAction)
                .addAction(R.drawable.ic_skip_next_black_24dp
                        , getString(R.string.action_next), getPlayBackAction(CODE_NEXT));
        return builder.build();
    }

    private PendingIntent getPlayBackAction(int actionNumber) {
        Intent playBackAction = new Intent(this, MusicPlayerService.class);
        switch (actionNumber) {
            case CODE_PLAY:
                playBackAction.setAction(ACTION_PLAY);
                return PendingIntent.getService(this, actionNumber, playBackAction, 0);
            case CODE_PAUSE:
                playBackAction.setAction(ACTION_PAUSE);
                return PendingIntent.getService(this, actionNumber, playBackAction, 0);
            case CODE_NEXT:
                playBackAction.setAction(ACTION_NEXT);
                return PendingIntent.getService(this, actionNumber, playBackAction, 0);
            case CODE_PREVIOUS:
                playBackAction.setAction(ACTION_PREVIOUS);
                return PendingIntent.getService(this, actionNumber, playBackAction, 0);
            default:
                break;
        }
        return null;
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    private void handleIncomingActions(Intent playbackAction) {
        if (playbackAction == null || playbackAction.getAction() == null) return;
        String actionString = playbackAction.getAction();
        if (actionString.equalsIgnoreCase(ACTION_PLAY)) {
            resumeMusic();
        } else if (actionString.equalsIgnoreCase(ACTION_PAUSE)) {
            pauseMusic();
        } else if (actionString.equalsIgnoreCase(ACTION_NEXT)) {
            nextSong();
        } else if (actionString.equalsIgnoreCase(ACTION_PREVIOUS)) {
            previousSong();
        } else if (actionString.equalsIgnoreCase(ACTION_STOP)) {
            stopMusic();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    public void controlPlayPauseMusic() {
        if (mIsPlaying) pauseMusic();
        else resumeMusic();
    }

    public void controlPreviousSong() {
        previousSong();
    }

    public void controlNextSong() {
        nextSong();
    }

    @Override
    public void onClick(View v) {

    }

    public class LocalBinder extends Binder {
        MusicPlayerService getService() {
            return MusicPlayerService.this;
        }
    }

    public interface OnPlaybackStatusChangeListener {
        void playbackStatusChange(boolean isPlaying);

        void updateCurrentTime(int position);

        void updateSongImageAndDuration(byte[] songImage, int duration);
    }
}
