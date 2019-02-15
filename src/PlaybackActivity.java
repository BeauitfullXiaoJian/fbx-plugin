package com.fbx;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.dobay.dudao.R;
import com.videogo.exception.BaseException;
import com.videogo.openapi.EZConstants;
import com.videogo.openapi.EZOpenSDK;
import com.videogo.openapi.EZPlayer;
import com.videogo.openapi.bean.EZDeviceRecordFile;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class PlaybackActivity extends AppCompatActivity implements
        View.OnClickListener,
        DatePicker.OnDateChangedListener,
        ScalableTimeBar.OnBarMoveListener,
        SeekBar.OnSeekBarChangeListener,
        SurfaceHolder.Callback,
        Handler.Callback {

    private static final String TAG = "PlaybackActivity";
    private static final String TAG_START_TIME = "TAG_START_TIME";
    private static final String TAG_END_TIME = "TAG_END_TIME";
    private String mCameraName;
    private String mCameraSeries;
    private int mCameraNo;
    private TextView mDateView;
    private DatePicker mDatePicker;
    private ScalableTimeBar mScalableTimeBar;
    private View mPickerView;
    private Button mStartBtn;
    private Button mEndBtn;
    private ImageView mRecordBtn;
    private ImageView mPlayBtn;
    private SurfaceView mPlayView;
    private Calendar mStartTime;
    private Calendar mEndTime;
    private EZPlayer mEZPlayer;
    private View mPlayContainer;
    private ProgressBar mLoadingBar;
    private int mPlayStatus = 0;
    private boolean isRecord = Boolean.FALSE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playback);
        initSDK();
        initView();
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "按下返回");
        Configuration configuration = getResources().getConfiguration();
        if (mPickerView.getVisibility() == View.VISIBLE) {
            Log.d(TAG, "关闭日期选择窗口");
            closeDateDialog();
        } else if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Log.d(TAG, "退出全屏");
            setDefaultScreen();
        } else {
            Log.d(TAG, "关闭当前活动");
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mEZPlayer != null) {
            mEZPlayer.release();
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
        if (mEZPlayer != null) {
            Log.d(TAG, "恢复播放");
            mEZPlayer.setSurfaceHold(surfaceHolder);
            if (mPlayStatus == 1) {
                mEZPlayer.resumePlayback();
            }
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        if (mEZPlayer != null) {
            Log.d(TAG, "暂停播放");
            mEZPlayer.setSurfaceHold(null);
            if (mPlayStatus != 2) {
                mEZPlayer.pausePlayback();
            }
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_start_time: {
                openDateDialog(TAG_START_TIME, mStartTime);
                break;
            }
            case R.id.btn_end_time: {
                openDateDialog(TAG_END_TIME, mEndTime);
                break;
            }
            case R.id.btn_camera: {
                if (isRecord) {
                    stopRecord();
                } else {
                    startRecord();
                }
                break;
            }
            case R.id.btn_snapshot: {
                if (mEZPlayer != null) {
                    // 请求保存图片，需要获取写入权限
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        requestPermissions(new String[]{
                                Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
                    } else {
                        savePicture();
                    }
                }
                break;
            }
            case R.id.btn_play: {
                if (mPlayStatus == 1) {
                    pause();
                    setStopStatus();
                } else if (mPlayStatus == 2) {
                    play();
                    setPlayStatus();
                }
                break;
            }
            case R.id.btn_fullscreen: {
                setFullScreen();
                break;
            }
        }
    }

    @Override
    public void onDateChanged(DatePicker datePicker, int year, int month, int day) {
        Log.d(TAG, "日期改变");
        if (datePicker.getTag().equals(TAG_START_TIME)) {
            mStartTime = Calendar.getInstance();
            mStartTime.set(year, month, day);
            mStartBtn.setText(LoadSDCardVideoTask.formatCalendarDate(mStartTime));

        } else {
            mEndTime = Calendar.getInstance();
            mEndTime.set(year, month, day);
            mEndBtn.setText(LoadSDCardVideoTask.formatCalendarDate(mEndTime));
        }
        closeDateDialog();
        searchPlaybackVideo();
    }

    @Override
    public void onBarMove(long screenLeftTime, long screenRightTime, long currentTime) {
        Log.d(TAG, "移动位置-当前时间" + currentTime);
        updateDateView(currentTime);
    }

    @Override
    public void OnBarMoveFinish(long screenLeftTime, long screenRightTime, long currentTime) {
        Log.d(TAG, "移动结束-当前时间,启用播放" + currentTime);
        Calendar startTime = Calendar.getInstance();
        Calendar endTime = Calendar.getInstance();
        startTime.setTimeInMillis(currentTime);
        endTime.setTimeInMillis(screenRightTime);
        startPlay(startTime, endTime);
    }

    @Override
    public boolean handleMessage(Message message) {
        // 不管成功或失败都关闭加载动画
        hideLoading();
        Log.d(TAG, "接收到消息" + message.what);
        switch (message.what) {
            case EZConstants.EZPlaybackConstants.MSG_REMOTEPLAYBACK_PLAY_START: {
                Log.d(TAG, "开始播放");
                setPlayStatus();
                mEZPlayer.openSound();
                break;
            }
            case EZConstants.EZPlaybackConstants.MSG_REMOTEPLAYBACK_PLAY_SUCCUSS: {
                Log.d(TAG, "播放成功");
                setPlayStatus();
                mEZPlayer.openSound();
                break;
            }
            case EZConstants.EZPlaybackConstants.MSG_REMOTEPLAYBACK_PLAY_FINISH: {
                Log.d(TAG, "播放结束-正常结束");
                setStopStatus();
                break;
            }
            case EZConstants.EZPlaybackConstants.MSG_REMOTEPLAYBACK_PLAY_FAIL: {
                Log.d(TAG, "播放结束-无法继续");
                setStopStatus();
                break;
            }
            case EZConstants.EZPlaybackConstants.MSG_CAPTURE_PICTURE_SUCCESS: {
                showToast("截图成功,请到相册查看图片");
                break;
            }
        }
        return false;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (mEZPlayer != null) {
            mEZPlayer.openSound();
        }

        if (mEZPlayer != null || i > 0) {
            am.setStreamVolume(AudioManager.STREAM_MUSIC, i, 0);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "成功授权，可以写入本地文件");
            savePicture();
        } else {
            Log.d(TAG, "拒绝授权，无法写入本地文件");
            showToast("图片保存失败，需要本地存储权限");
        }
    }

    private void closeDateDialog() {
        mPickerView.setVisibility(View.INVISIBLE);
    }

    private void openDateDialog(String tagName, Calendar calendar) {
        mDatePicker.setTag(tagName);
        mPickerView.setVisibility(View.VISIBLE);
        if (calendar != null) {
            mDatePicker.init(calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH),
                    PlaybackActivity.this);
        }
    }

    private void searchPlaybackVideo() {
        if (mStartTime != null && mEndTime != null) {
            LoadSDCardVideoTask task = LoadSDCardVideoTask.newInstance(mCameraSeries, mCameraNo, PlaybackActivity.this);
            task.execute(mStartTime, mEndTime);
        }
    }

    private void startPlay(Calendar startTime, Calendar endTime) {
        if (mEZPlayer == null) {
            mEZPlayer = EZOpenSDK.getInstance().createPlayer(mCameraSeries, mCameraNo);
            if (mEZPlayer == null) {
                // 获取失败？？？？
                Log.d(TAG, "播放器是空的");
                showToast("萤石云服务异常～");
            }
             mEZPlayer.setSurfaceHold(mPlayView.getHolder());
             mEZPlayer.setHandler(new Handler(PlaybackActivity.this));
        }
        mEZPlayer.startPlayback(startTime, endTime);
        showLoading();
    }

    private void savePicture() {
        try {
            Bitmap bitmap = mEZPlayer.capturePicture();
            if (bitmap != null) {
                Log.d(TAG, "图片大小" + bitmap.getByteCount());
                MediaStore.Images.Media.insertImage(getContentResolver(),
                        bitmap, "截图快照", "这个图片来源于督贝");
                bitmap.recycle();
                showToast("图片保存成功");
            }
        } catch (Exception e) {
            e.printStackTrace();
            showToast("截图失败");
        }
    }

    private void play() {
        if (mEZPlayer != null) {
            mEZPlayer.resumePlayback();
        }
    }

    private void pause() {
        if (mEZPlayer != null) {
            mEZPlayer.pausePlayback();
        }
    }

    private void startRecord() {
        if (mEZPlayer != null) {
            String filePath = DataManager.getRecordPath(PlaybackActivity.this);
            if (mEZPlayer.startLocalRecordWithFile(filePath)) {
                showToast("开始录像");
                mRecordBtn.setImageDrawable(getResources().getDrawable(R.drawable.btn_video_selected));
                isRecord = Boolean.TRUE;
            } else {
                showToast("录像失败，当前状态无法录像操作");
            }
        } else {
            showToast("当前还没有播放任何录像，无法录制～");
        }

    }

    private void stopRecord() {
        if (mEZPlayer != null) {
            mEZPlayer.stopLocalRecord();
            mRecordBtn.setImageDrawable(getResources().getDrawable(R.drawable.btn_video_normal));
            showToast("录像结束～");
            isRecord = Boolean.FALSE;
        }
    }

    private void setPlayStatus() {
        mPlayStatus = 1;
        mPlayBtn.setImageDrawable(getResources().getDrawable(R.drawable.btn_stop_n));
    }

    private void setStopStatus() {
        mPlayStatus = 2;
        mPlayBtn.setImageDrawable(getResources().getDrawable(R.drawable.btn_play_n));
    }

    /**
     * 设置默认（竖屏）状态
     */
    private void setDefaultScreen() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        DisplayMetrics displaymetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        ViewGroup.LayoutParams layoutParams = mPlayContainer.getLayoutParams();
        layoutParams.height = (int) (displaymetrics.widthPixels / (16.0 / 9));
        mPlayContainer.setLayoutParams(layoutParams);
    }

    /**
     * 设置全屏状态
     */
    private void setFullScreen() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        DisplayMetrics displaymetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        ViewGroup.LayoutParams layoutParams = mPlayContainer.getLayoutParams();
        layoutParams.height = displaymetrics.heightPixels;
        mPlayContainer.setLayoutParams(layoutParams);
    }

    private void updateDateView(long time) {
        SimpleDateFormat zeroTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CANADA);
        mDateView.setText(zeroTimeFormat.format(time));
    }

    private void showLoading() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mLoadingBar.setVisibility(View.VISIBLE);
            }
        });
    }

    private void hideLoading() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mLoadingBar.setVisibility(View.INVISIBLE);
            }
        });
    }

    private void initView() {
        Calendar calendar = Calendar.getInstance();
        mDatePicker = (DatePicker) findViewById(R.id.calendar);
        mDatePicker.init(
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH),
                PlaybackActivity.this);
        mDateView = (TextView) findViewById(R.id.date_view);
        mPlayContainer = findViewById(R.id.playback_content);
        mPlayView = (SurfaceView) findViewById(R.id.playback_view);
        mPlayView.getHolder().addCallback(PlaybackActivity.this);
        mPickerView = findViewById(R.id.calendar_container);
        mStartBtn = (Button) findViewById(R.id.btn_start_time);
        mEndBtn = (Button) findViewById(R.id.btn_end_time);
        mStartBtn.setOnClickListener(PlaybackActivity.this);
        mEndBtn.setOnClickListener(PlaybackActivity.this);
        findViewById(R.id.btn_snapshot).setOnClickListener(PlaybackActivity.this);
        mRecordBtn = (ImageView) findViewById(R.id.btn_camera);
        mRecordBtn.setOnClickListener(PlaybackActivity.this);
        findViewById(R.id.btn_fullscreen).setOnClickListener(PlaybackActivity.this);
        mPlayBtn = (ImageView) findViewById(R.id.btn_play);
        mPlayBtn.setOnClickListener(PlaybackActivity.this);
        mLoadingBar = (ProgressBar) findViewById(R.id.loading_bar);
        final TextView textView = (TextView) findViewById(R.id.camera_title);
        textView.setText(mCameraName);
        mScalableTimeBar = (ScalableTimeBar) findViewById(R.id.time_bar);
        mScalableTimeBar.setOnBarMoveListener(PlaybackActivity.this);
        final SeekBar soundBar = (SeekBar) findViewById(R.id.sound_bar);
        soundBar.setOnSeekBarChangeListener(PlaybackActivity.this);
        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int maxVolume = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int currentVolume = am.getStreamVolume(AudioManager.STREAM_MUSIC);
        soundBar.setMax(maxVolume);
        soundBar.setProgress(currentVolume);
        setDefaultScreen();
    }

    private void initSDK() {
        Intent intent = getIntent();
        String appKey = intent.getStringExtra("appKey");
        String accessToken = intent.getStringExtra("accessToken");
        EZOpenSDK.showSDKLog(true);
        EZOpenSDK.enableP2P(false);
        EZOpenSDK.initLib(getApplication(), appKey);
        EZOpenSDK.getInstance().setAccessToken(accessToken);
        mCameraName = intent.getStringExtra("cameraName");
        mCameraSeries = intent.getStringExtra("cameraSeries");
        mCameraNo = intent.getIntExtra("cameraNo", 1);
    }

    private void showToast(final String message) {
        runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public static class DataManager {

        static String getRecordPath(Context context) {
            String appName = context.getString(R.string.app_name);
            String rootPath = Environment.getExternalStorageDirectory().toString() + "/" + appName + "/";
            File file = new File(rootPath);
            if (file.isFile() || !file.exists()) {
                file.mkdirs();
            }
            return rootPath + Calendar.getInstance().getTimeInMillis() + ".mp4";
        }
    }

    public static class LoadSDCardVideoTask extends AsyncTask<Calendar, Void, List<EZDeviceRecordFile>> {

        private String cameraSeries;
        private int cameraNo;
        private PlaybackActivity activity;

        static LoadSDCardVideoTask newInstance(String cameraSeries, int cameraNo,
                                               PlaybackActivity activity) {
            return new LoadSDCardVideoTask(cameraSeries, cameraNo, activity);
        }

        static String formatCalendarTime(Calendar calendar) {
            return String.format(Locale.CHINA, "%d:%d:%d",
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    calendar.get(Calendar.SECOND)
            );
        }

        static String formatCalendarDate(Calendar calendar) {
            return String.format(Locale.CHINA, "%d/%d/%d",
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH) + 1,
                    calendar.get(Calendar.DAY_OF_MONTH));
        }

        LoadSDCardVideoTask(String cameraSeries, int cameraNo, PlaybackActivity activity) {
            this.cameraSeries = cameraSeries;
            this.cameraNo = cameraNo;
            this.activity = activity;
        }

        @Override
        protected List<EZDeviceRecordFile> doInBackground(Calendar... calendars) {
            List<EZDeviceRecordFile> list = new ArrayList<EZDeviceRecordFile>();
            activity.showLoading();
            try {
                Log.d(TAG, "设备序列号:" + cameraSeries);
                list = EZOpenSDK.getInstance().searchRecordFileFromDevice(cameraSeries, cameraNo, calendars[0], calendars[1]);
                Log.d(TAG, "加载存储文件成功-共" + list.size());
                for (EZDeviceRecordFile file : list) {
                    Calendar startTime = file.getStartTime();
                    Calendar endTime = file.getStopTime();
                    Log.d(TAG, String.format(Locale.CHINA, "文件时长:%s-%s",
                            formatCalendarTime(startTime),
                            formatCalendarTime(endTime)));
                }
            } catch (BaseException e) {
                e.printStackTrace();
                activity.showToast(e.toString());
            } finally {
                activity.hideLoading();
            }
            return list;
        }

        @Override
        protected void onPostExecute(List<EZDeviceRecordFile> files) {
            if (files.size() > 0) {
                EZDeviceRecordFile firstFile = files.get(0);
                EZDeviceRecordFile lastFile = files.get(files.size() - 1);
                activity.mScalableTimeBar.initTimebarLengthAndPosition(
                        firstFile.getStartTime().getTimeInMillis(),
                        lastFile.getStartTime().getTimeInMillis(),
                        firstFile.getStartTime().getTimeInMillis()
                );
            }
            List<ScalableTimeBar.RecordDataExistTimeSegment> recordDataList = new ArrayList<ScalableTimeBar.RecordDataExistTimeSegment>();
            for (EZDeviceRecordFile file : files) {
                recordDataList.add(new ScalableTimeBar.RecordDataExistTimeSegment(
                        file.getStartTime().getTimeInMillis(),
                        file.getStopTime().getTimeInMillis()
                ));
            }
            activity.mScalableTimeBar.setRecordDataExistTimeClipsList(recordDataList);
        }
    }
}
