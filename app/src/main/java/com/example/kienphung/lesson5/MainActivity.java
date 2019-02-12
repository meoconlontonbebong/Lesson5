package com.example.kienphung.lesson5;

import android.animation.ObjectAnimator;
import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import de.hdodenhof.circleimageview.CircleImageView;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, MusicPlayerService.OnPlaybackStatusChangeListener
        , ServiceConnection, SeekBar.OnSeekBarChangeListener {
    private MusicPlayerService mMusicPlayerService;
    private ImageView mImagePlayPause;
    private CircleImageView mSongImage;
    private ImageView mBackground;
    private Handler mHandler;
    private SeekBar mSeekBar;
    private TextView mTextCurrentTime, mTextMaxTime;
    private boolean mIsPlaying;
    private int mCurrentTime;
    private ObjectAnimator mObjectAnimator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mHandler = new Handler();
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mIsPlaying) {
                    mCurrentTime += 1000;
                    mSeekBar.setProgress(mCurrentTime);
                    mTextCurrentTime.setText(convertTime(mCurrentTime));
                }
                mHandler.postDelayed(this, 1000);
            }
        });
        setUpView();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this, MusicPlayerService.class);
        bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
        setUpView();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
        unbindService(mServiceConnection);
    }


    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.image_play_pause:
                mMusicPlayerService.controlPlayPauseMusic();
                break;
            case R.id.image_previous:
                mObjectAnimator.end();
                mMusicPlayerService.controlPreviousSong();
                break;
            case R.id.image_next:
                mObjectAnimator.end();
                mMusicPlayerService.controlNextSong();
                break;
            default:
                break;

        }

    }

    private void setUpView() {
        mImagePlayPause = findViewById(R.id.image_play_pause);
        mImagePlayPause.setOnClickListener(this);
        ImageView imagePrevious = findViewById(R.id.image_previous);
        imagePrevious.setOnClickListener(this);
        ImageView imageNext = findViewById(R.id.image_next);
        imageNext.setOnClickListener(this);
        mSongImage = findViewById(R.id.image_song);
        mSeekBar = findViewById(R.id.seek_bar_time_playing);
        mTextCurrentTime = findViewById(R.id.text_current_time);
        mTextMaxTime = findViewById(R.id.text_song_length);
        mSeekBar.setOnSeekBarChangeListener(this);
        mBackground = findViewById(R.id.image_background);
        mObjectAnimator = ObjectAnimator.ofFloat(mSongImage, "rotation", 0, 360);
        mObjectAnimator.setDuration(15000);
        mObjectAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        mObjectAnimator.setInterpolator(new LinearInterpolator());
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void setUpPlayPauseImage(boolean isPlaying) {
        if (isPlaying) {
            mImagePlayPause.setImageResource(R.drawable.ic_pause_black_24dp);
            mObjectAnimator.resume();
        } else {
            mImagePlayPause.setImageResource(R.drawable.ic_play_black_24dp);
            mObjectAnimator.pause();
        }
    }

    private ServiceConnection mServiceConnection = this;

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void playbackStatusChange(boolean isPlaying) {
        mIsPlaying = isPlaying;
        setUpPlayPauseImage(isPlaying);
    }

    @Override
    public void updateCurrentTime(int position) {
        mCurrentTime = position;
        mSeekBar.setProgress(position);
        mTextCurrentTime.setText(convertTime(position));
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Override
    public void updateSongImageAndDuration(byte[] songImage, int duration) {
        Bitmap picture = BitmapFactory.decodeByteArray(songImage, 0, songImage.length);


        mSongImage.setImageBitmap(picture);
        Bitmap blurBackGround = BlurBuilder.blur(this, picture);
        mBackground.setImageBitmap(blurBackGround);


        mSeekBar.setMax(duration);
        mTextMaxTime.setText(convertTime(duration));
        mCurrentTime = 0;
        mObjectAnimator.start();
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        MusicPlayerService.LocalBinder binder = (MusicPlayerService.LocalBinder) iBinder;
        mMusicPlayerService = binder.getService();
        mMusicPlayerService.setOnPlaybackStatusChangeListener(this);
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        mMusicPlayerService.setOnPlaybackStatusChangeListener(null);
    }

    private String convertTime(int time) {
        time /= 1000;
        int minute = time / 60;
        int second = time % 60;
        String result = "";
        result += minute < 10 ? ("0" + minute) : minute;
        result += ":";
        result += second < 10 ? ("0" + second) : second;
        return result;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
        if (b)
            updateCurrentTime(i);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        mMusicPlayerService.seekTo(seekBar.getProgress());
    }
}
