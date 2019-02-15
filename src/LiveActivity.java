package com.fbx;

import android.animation.ValueAnimator;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.CardView;
import android.support.v7.widget.GridLayoutManager;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonObject;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.squareup.picasso.Picasso;
import com.videogo.errorlayer.ErrorInfo;
import com.videogo.openapi.EZConstants;
import com.videogo.openapi.EZOpenSDK;
import com.videogo.openapi.EZPlayer;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.dobay.dudao.R;

import okhttp3.Call;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class LiveActivity extends AppCompatActivity implements Handler.Callback, okhttp3.Callback, View.OnClickListener,
        View.OnTouchListener, SurfaceHolder.Callback {

    protected final String TAG = "LiveActivityLog";
    public final int PLAY_PREPARE = 0;
    public final int PLAY_PLAYING = 1;
    public final int PLAY_PAUSE = 2;

    public static String mUploadUrl;
    public static String mFormUrl;
    public static String mApiData;
    public static String sAppKey;
    public static String sAccessToken;
    public EZPlayer mActivePlayer;
    public EZPlayer mActiveTalker;
    private int mPlayerStatus;
    private boolean mToolPadActive;
    public CameraData mActiveCamera;
    public StoreData mStoreData;
    private List<CameraData> mCameras = new ArrayList<CameraData>();

    private View mPlayContainerView;
    private SurfaceView mSurfaceView;
    private View mHeadCtrl;
    private View mBottomCtrl;
    private ImageView mPlayBtn;
    private ImageView mToolBtn;
    private View mToolPadCtrl;
    private View mPrepareView;
    private View mRecoverPlayView;
    private ImageView mPauseView;

    private Handler mMessageHandler = new Handler(LiveActivity.this);
    private Integer mDisappearHandlerCx = 0;
    private Handler mDisappearHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            if (msg.what == mDisappearHandlerCx && mToolPadCtrl.getHeight() < 50) {
                AlphaAnimation disappearAnimation = new AlphaAnimation(1, 0);
                disappearAnimation.setDuration(1000);
                disappearAnimation.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        mHeadCtrl.setVisibility(View.INVISIBLE);
                        mBottomCtrl.setVisibility(View.INVISIBLE);
                    }
                });
                mHeadCtrl.startAnimation(disappearAnimation);
                mBottomCtrl.startAnimation(disappearAnimation);
            }
            return false;
        }
    });

    @Override
    public void onResponse(Call call, Response response) throws IOException {
        Log.d(TAG, "请求回调成功");
        ResponseBody responseBody = response.body();
        if (responseBody != null) {
            String apiString = responseBody.string();
            Log.d(TAG, apiString);
            ApiResponse apiResponse = new ApiResponse(apiString);
            showToast(apiResponse.getMessage());
        }
    }

    @Override
    public void onFailure(Call call, IOException e) {
        Log.d(TAG, "请求失败");
        showToast("数据通信异常，请检查您的网络");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live);
        if (savedInstanceState != null) {
            initActivitySaved(savedInstanceState);
        } else {
            initActivityDefault();
        }
        initView();
        hidePlayControl();
        setPrepareMode();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        Log.d(TAG, "SAVE_DATA");
        super.onSaveInstanceState(outState);
        outState.putSerializable(TAG, CameraDataTool.cardListToJsonString(mCameras));
        outState.putSerializable(TAG + "STORE", mStoreData);
        if (mActiveCamera != null) {
            outState.putSerializable(TAG + "ACTIVE", mActiveCamera.getCameraSns());
        }
    }

    @Override
    public boolean handleMessage(Message message) {
        switch (message.what) {
            case EZConstants.EZRealPlayConstants.MSG_REALPLAY_PLAY_SUCCESS:
                Log.d(TAG, "成功连接到摄像头");
                mActivePlayer.openSound();
                setPlayingMode();
                break;
            case EZConstants.EZRealPlayConstants.MSG_REALPLAY_PLAY_FAIL:
                ErrorInfo errorinfo = (ErrorInfo) message.obj;
                String codeStr = errorinfo.moduleCode;
                String description = errorinfo.description;
                String moreInfo = errorinfo.sulution;
                Log.d(TAG, "直播失败");
                Log.d(TAG, codeStr + description + moreInfo);
                SystemClock.sleep(500);
                mActivePlayer.startRealPlay();
                break;
        }
        return false;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            // 按下播放|暂停按钮
            case R.id.btn_play: {
                if (mPlayerStatus == PLAY_PREPARE) {
                    showToast("摄像头还未就绪，请等待");
                } else if (mPlayerStatus == PLAY_PAUSE) {
                    Log.d(TAG, "恢复播放");
                    mActivePlayer.startRealPlay();
                    setRecoverMode();
                } else {
                    Log.d(TAG, "暂停播放");
                    mPauseView.setImageBitmap(mActivePlayer.capturePicture());
                    mActivePlayer.stopRealPlay();
                    showToast("直播已暂停");
                    setPauseMode();
                }
                break;
            }
            // 按下展开工具栏按钮
            case R.id.btn_popwindow: {
                if (mToolPadActive) {
                    Log.d(TAG, "收起工具栏");
                    hideToolPad();
                    hidePlayControl();
                } else {
                    Log.d(TAG, "展开工具栏");
                    showToolPad();
                }
                break;
            }
            // 进入回放页面
            case R.id.menu_playback: {
                if (mActiveCamera == null) {
                    showToast("当前没有可用摄像头，无法回放");
                    return;
                }
                Intent intent = new Intent(this, PlaybackActivity.class);
                intent.putExtra("cameraName", mActiveCamera.getCameraTitle());
                intent.putExtra("cameraSeries", mActiveCamera.getCameraSns());
                intent.putExtra("cameraNo", mActiveCamera.getCameraNo());
                intent.putExtra("appKey", sAppKey);
                intent.putExtra("accessToken", sAccessToken);
                startActivity(intent);
                break;
            }
            // 弹出语音控制台
            case R.id.menu_talk: {
                Log.d(TAG, "语言对讲");
                if (mActiveTalker == null) {
                    showToast("当前没有可用的摄像头");
                } else {
                    new TalkPopupWindow(LiveActivity.this)
                            .showAtLocation(findViewById(R.id.main_view),
                                    Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);
                }
                break;
            }
            // 弹出云台控制
            case R.id.menu_control: {
                Log.d(TAG, "云台控制");
                if (mActivePlayer == null) {
                    showToast("当前没有可用的摄像头");
                } else if (mPlayerStatus == PLAY_PREPARE) {
                    showToast("摄像头还未就绪，请等待");
                } else if (mPlayerStatus == PLAY_PAUSE) {
                    showToast("直播已经暂停，无法进行云台控制");
                } else {
                    new ControlPopupWindow(LiveActivity.this)
                            .showAtLocation(findViewById(R.id.main_view),
                                    Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);
                }
                break;
            }
            // 弹出抓图考评
            case R.id.menu_check: {
                Log.d(TAG, "抓图");
                if (mPlayerStatus != PLAY_PLAYING) {
                    showToast("当前不是直播状态，无法进行抓图操作");
                    return;
                }
                Bitmap bitmap = mActivePlayer.capturePicture();
                new CutPopupWindow(LiveActivity.this, bitmap)
                        .showAtLocation(findViewById(R.id.main_view),
                                Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 0);
                break;
            }
            // 截图
            case R.id.menu_cut: {
                Log.d(TAG, "截图操作");
                if (mPlayerStatus != PLAY_PLAYING) {
                    showToast("当前不是直播状态，无法进行抓图操作");
                    return;
                }
                Bitmap bitmap = mActivePlayer.capturePicture();
                showToast("快照截取成功，正在上传");
                uploadFile(bitmap);
                break;
            }
            // 全屏|收起
            case R.id.btn_fullscreen: {
                Configuration mConfiguration = getResources().getConfiguration();
                if (mConfiguration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                } else {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                }
                break;
            }
            // 返回按钮
            case R.id.btn_back: {
                Configuration mConfiguration = getResources().getConfiguration();
                if (mConfiguration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                } else {
                    LiveActivity.this.finish();
                }
                break;
            }
        }
    }

    @Override
    public void onBackPressed() {
        findViewById(R.id.btn_back).callOnClick();
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        switch (view.getId()) {
            case R.id.play_container: {
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    showPlayControl();
                }
                if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    hidePlayControl();
                }
                break;
            }
            case R.id.talk_btn: {

            }
        }
        return false;
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        startPlayer();
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        if (mActivePlayer != null) {
            mActivePlayer.release();
        }
        if (mActiveTalker != null) {
            mActiveTalker.release();
        }
    }

    /**
     * 通过获取页面传递参数初始化Activity
     */
    private void initActivityDefault() {
        Log.d(TAG, "默认初始化Activity");
        Intent intent = getIntent();
        String appKey = intent.getStringExtra("appKey");
        String accessToken = intent.getStringExtra("accessToken");
        mStoreData = (StoreData) intent.getSerializableExtra("storeData");
        mUploadUrl = intent.getStringExtra("uploadUrl");
        mFormUrl = intent.getStringExtra("formUrl");
        mApiData = intent.getStringExtra("apiData");
        mCameras = CameraDataTool.getCardListFromJsonString(intent.getStringExtra("cameraData"));
        for (CameraData camera : mCameras) {
            if (camera.getOnline()) {
                mActiveCamera = camera;
                break;
            }
        }
        initSDK(appKey, accessToken);
    }

    /**
     * 通过保存的快照数据初始化Activity
     */
    private void initActivitySaved(Bundle savedInstanceState) {
        String jsonString = savedInstanceState.getString(TAG);
        String cameraSn = savedInstanceState.getString(TAG + "ACTIVE", "NONE");
        mStoreData = (StoreData) savedInstanceState.getSerializable(TAG + "STORE");
        mCameras = CameraDataTool.getCardListFromJsonString(jsonString);
        if (!cameraSn.equals("NONE")) {
            mActiveCamera = new CameraData(cameraSn);
            for (CameraData camera : mCameras) {
                if (camera.getCameraSns().equals(mActiveCamera.getCameraSns())) {
                    mActiveCamera = camera;
                    break;
                }
            }
        }
        initView();
    }

    /**
     * 初始化视图
     */
    private void initView() {
        mPlayContainerView = findViewById(R.id.play_container);
        mSurfaceView = (SurfaceView) findViewById(R.id.play_view);
        mHeadCtrl = findViewById(R.id.play_head);
        mBottomCtrl = findViewById(R.id.play_control);
        mPlayBtn = (ImageView) findViewById(R.id.btn_play);
        mToolBtn = (ImageView) findViewById(R.id.btn_popwindow);
        mToolPadCtrl = findViewById(R.id.tool_pad);
        mPrepareView = findViewById(R.id.play_loading);
        mRecoverPlayView = findViewById(R.id.recover_loading_bar);
        mPauseView = (ImageView) findViewById(R.id.recover_holder_image);
        mSurfaceView.getHolder().addCallback(LiveActivity.this);
        mPlayBtn.setOnClickListener(LiveActivity.this);
        mToolBtn.setOnClickListener(LiveActivity.this);
        mPlayContainerView.setOnTouchListener(LiveActivity.this);
        findViewById(R.id.btn_fullscreen).setOnClickListener(LiveActivity.this);
        findViewById(R.id.btn_back).setOnClickListener(LiveActivity.this);
        findViewById(R.id.menu_playback).setOnClickListener(LiveActivity.this);
        findViewById(R.id.menu_check).setOnClickListener(LiveActivity.this);
        findViewById(R.id.menu_talk).setOnClickListener(LiveActivity.this);
        findViewById(R.id.menu_control).setOnClickListener(LiveActivity.this);
        findViewById(R.id.menu_cut).setOnClickListener(LiveActivity.this);
        GridLayoutManager layoutManager = new GridLayoutManager(getBaseContext(), 2);
        CardAdapter adapter = new CardAdapter();
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.camera_list);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);
        Configuration mConfiguration = getResources().getConfiguration();
        ViewGroup.LayoutParams layoutParams = mPlayContainerView.getLayoutParams();
        if (mConfiguration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
            mToolBtn.setVisibility(View.GONE);
        }
        ((TextView) findViewById(R.id.store_title)).setText(mStoreData.getStoreTitle());
        ((TextView) findViewById(R.id.subject_title)).setText("所有监控点(" + mCameras.size() + ")");
    }

    /**
     * 初始化SDK工具
     */
    private void initSDK(String appKey, String accessToken) {
        EZOpenSDK.showSDKLog(true);
        EZOpenSDK.enableP2P(false);
        EZOpenSDK.initLib(getApplication(), appKey);
        EZOpenSDK.getInstance().setAccessToken(accessToken);
        sAppKey = appKey;
        sAccessToken = accessToken;
    }

    /**
     * 初始化一个播放器
     *
     * @param deviceSerial 设备序列号
     * @param cameraNo     设备编号一般都是(1)
     */
    private void initPlayer(String deviceSerial, int cameraNo) {
        if (mActivePlayer != null) {
            mActivePlayer.release();
            mActiveTalker.release();
        }
        Log.d(TAG, "初始化播放器");
        mPlayerStatus = PLAY_PREPARE;
        mActivePlayer = EZOpenSDK.getInstance().createPlayer(deviceSerial, cameraNo);
        mActiveTalker = EZOpenSDK.getInstance().createPlayer(deviceSerial, cameraNo);
        mActivePlayer.setHandler(mMessageHandler);
        mActivePlayer.setSurfaceHold(mSurfaceView.getHolder());
    }

    /**
     * 开始播放摄像头数据
     */
    private void startPlayer() {
        if (mActiveCamera == null) {
            showToast("当前没有可用摄像头");
        } else if (!mActiveCamera.getOnline()) {
            showToast("当前摄像头处于离线状态");
        } else {
            initPlayer(mActiveCamera.getCameraSns(), mActiveCamera.getCameraNo());
            mActivePlayer.startRealPlay();
        }
    }

    private void setPrepareMode() {
        mPlayerStatus = PLAY_PREPARE;
        mPlayBtn.setImageDrawable(getResources().getDrawable(R.drawable.ic_pause));
        mPrepareView.setVisibility(View.VISIBLE);
        mPauseView.setVisibility(View.INVISIBLE);
        mRecoverPlayView.setVisibility(View.INVISIBLE);
    }

    private void setRecoverMode() {
        mPlayerStatus = PLAY_PREPARE;
        mPlayBtn.setImageDrawable(getResources().getDrawable(R.drawable.ic_pause));
        mPrepareView.setVisibility(View.INVISIBLE);
        mPauseView.setVisibility(View.INVISIBLE);
        mRecoverPlayView.setVisibility(View.VISIBLE);
    }

    private void setPlayingMode() {
        mPlayerStatus = PLAY_PLAYING;
        mPlayBtn.setImageDrawable(getResources().getDrawable(R.drawable.ic_pause));
        mPrepareView.setVisibility(View.INVISIBLE);
        mPauseView.setVisibility(View.INVISIBLE);
        mRecoverPlayView.setVisibility(View.INVISIBLE);
    }

    private void setPauseMode() {
        mPlayerStatus = PLAY_PAUSE;
        mPlayBtn.setImageDrawable(getResources().getDrawable(R.drawable.ic_start));
        mPrepareView.setVisibility(View.INVISIBLE);
        mPauseView.setVisibility(View.VISIBLE);
        mRecoverPlayView.setVisibility(View.INVISIBLE);
    }

    /**
     * 收起所有的播放控件
     */
    private void hidePlayControl() {
        mDisappearHandler.sendEmptyMessageDelayed(++mDisappearHandlerCx, 5000);
    }

    /**
     * 显示所有的播放控件
     */
    private void showPlayControl() {
        mHeadCtrl.setVisibility(View.VISIBLE);
        mBottomCtrl.setVisibility(View.VISIBLE);
    }

    /**
     * 显示工具面板
     */
    private void showToolPad() {
        mToolPadActive = true;
        ValueAnimator animator = ValueAnimator.ofInt(15, 260);
        RotateAnimation animation = new RotateAnimation(0, 180,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
                0.5f);
        animation.setDuration(500);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(ValueAnimator animator) {
                int value = (Integer) animator.getAnimatedValue();
                ViewGroup.LayoutParams layoutParams = mToolPadCtrl.getLayoutParams();
                layoutParams.height = value;
                mToolPadCtrl.setAlpha((float) value / 260);
                mToolPadCtrl.setLayoutParams(layoutParams);
            }
        });
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                mToolPadCtrl.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                Drawable icon = getResources().getDrawable(R.drawable.ic_reduce);
                mToolBtn.setImageDrawable(icon);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        animator.start();
        mToolBtn.startAnimation(animation);
    }

    /**
     * 隐藏工具面板
     */
    private void hideToolPad() {
        mToolPadActive = false;
        ValueAnimator animator = ValueAnimator.ofInt(260, 15);
        RotateAnimation animation = new RotateAnimation(0, 180,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
                0.5f);
        animation.setDuration(500);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(ValueAnimator animator) {
                int value = (Integer) animator.getAnimatedValue();
                ViewGroup.LayoutParams layoutParams = mToolPadCtrl.getLayoutParams();
                layoutParams.height = value;
                mToolPadCtrl.setAlpha((float) value / 260);
                mToolPadCtrl.setLayoutParams(layoutParams);
            }
        });
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                Drawable icon = getResources().getDrawable(R.drawable.ic_plus);
                mToolBtn.setImageDrawable(icon);
                mToolPadCtrl.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        animator.start();
        mToolBtn.startAnimation(animation);
    }

    /**
     * 显示提示消息
     *
     * @param message 消息内容
     */
    public void showToast(final String message) {
        runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }


    /**
     * 图片上传
     *
     * @param bitmap 要上传的图片
     */
    public void uploadFile(Bitmap bitmap) {
        try {
            String savePath = getFilesDir().getAbsolutePath();
            String filePath = savePath + "/" + System.currentTimeMillis() + ".jpg";
            Log.d(TAG, "快照保存路径:" + filePath);
            File file = new File(filePath);
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
            bos.flush();
            bos.close();
            JsonObject authObject = ApiResponse.getAuthHeader(mApiData);
            OkHttpClient client = new OkHttpClient();
            Log.d(TAG, String.valueOf(mStoreData.getStoreId()));
            MultipartBody multipartBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("shop_id", String.valueOf(mStoreData.getStoreId()))
                    .addFormDataPart("camera_id", String.valueOf(mActiveCamera.getCameraId()))
                    .addFormDataPart("file", file.getName(),
                            RequestBody.create(MultipartBody.FORM, file))
                    .build();
            Request request = new Request.Builder()
                    .addHeader("ng-params-one", authObject.get("ng-params-one").getAsString())
                    .addHeader("ng-params-two", authObject.get("ng-params-two").getAsString())
                    .addHeader("ng-params-three", authObject.get("ng-params-three").getAsString())
                    .url(mUploadUrl)
                    .post(multipartBody)
                    .build();
            Call call = client.newCall(request);
            call.enqueue(LiveActivity.this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 列表视图适配器
     */
    protected class CardAdapter extends RecyclerView.Adapter<CardAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            View view = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.camera_card, viewGroup, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {
            ViewGroup.MarginLayoutParams params =
                    (ViewGroup.MarginLayoutParams) viewHolder.cardView.getLayoutParams();
            if (i % 2 > 0) {
                params.leftMargin = 8;
            } else {
                params.rightMargin = 8;
            }
            CameraData cameraData = mCameras.get(i);
            viewHolder.cameraData = cameraData;
            ImageLoaderConfiguration config = new ImageLoaderConfiguration
                    .Builder(LiveActivity.this)
                    .build();
            ImageLoader.getInstance().init(config);
            ImageLoader imageLoader = ImageLoader.getInstance();
            if (cameraData.getOnline() && cameraData.getSnapshotUrl() != null) {
                try {
                    Picasso.get().load(cameraData.getSnapshotUrl())
                            .resize(600, 320)
                            .placeholder(R.drawable.ic_play)
                            .into(viewHolder.cardImageView);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                viewHolder.cardStatusView.setVisibility(View.VISIBLE);
            } else {
                Picasso.get().load(R.drawable.lost)
                        .resize(600, 320)
                        .into(viewHolder.cardImageView);
                viewHolder.cardStatusView.setVisibility(View.INVISIBLE);
            }
            viewHolder.cardTitleView.setText(cameraData.getCameraTitle());
        }

        @Override
        public int getItemCount() {
            return mCameras.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

            CameraData cameraData;
            CardView cardView;
            ImageView cardImageView;
            TextView cardTitleView;
            TextView cardStatusView;

            ViewHolder(View view) {
                super(view);
                cardView = (CardView) view;
                cardImageView = view.findViewById(R.id.camera_image);
                cardTitleView = view.findViewById(R.id.camera_title);
                cardStatusView = view.findViewById(R.id.camera_status);
                cardView.setOnClickListener(this);
            }

            @Override
            public void onClick(View v) {
                if (cameraData.getOnline()) {
                    mActiveCamera = cameraData;
                    setPrepareMode();
                    startPlayer();
                } else {
                    Toast.makeText(getApplicationContext(),
                            "当前摄像头暂不可用", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}