package com.fbx;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;

import com.videogo.openapi.EZConstants;
import com.videogo.openapi.EZOpenSDK;
import com.videogo.openapi.EZPlayer;
import com.videogo.openapi.bean.EZDeviceInfo;

import com.dobay.dudao.R;

public class TalkPopupWindow extends PopupWindow implements View.OnTouchListener {

    protected static final String TAG = "PopupWindowLog";

    // 窗口视图对象
    private View mPopupView;

    // 父Activity对象
    private LiveActivity mParentActivity;

    // 当前设备信息
    private EZDeviceInfo mDeviceInfo;

    public TalkPopupWindow(LiveActivity parentActivity) {
        super(parentActivity);
        mParentActivity = parentActivity;
        LayoutInflater inflater = (LayoutInflater) mParentActivity
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (inflater != null) {
            mPopupView = inflater.inflate(R.layout.popup_talk_control, null);
            setContentView(mPopupView);
            setWidth(ViewGroup.LayoutParams.MATCH_PARENT);
            setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
            setFocusable(true);
            setAnimationStyle(R.style.popmenu_animation);
            setBackgroundDrawable(new ColorDrawable(0));
            mPopupView.findViewById(R.id.talk_btn).setOnTouchListener(TalkPopupWindow.this);
            final CameraData camera = mParentActivity.mActiveCamera;
            new Thread(new Runnable() {
                public void run() {
                    try {

                        mDeviceInfo = EZOpenSDK.getInstance().getDeviceInfo(camera.getCameraSns());
                        mParentActivity.mActiveTalker.startVoiceTalk();
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.d(TAG, "获取设备信息失败");
                    }
                }
            }).start();
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (mDeviceInfo == null) {
            mParentActivity.showToast("当前设备还未就绪，无法通信");
            return false;
        }
        EZConstants.EZTalkbackCapability capability = mDeviceInfo.isSupportTalk();
        if (capability == EZConstants.EZTalkbackCapability.EZTalkbackNoSupport) {
            mParentActivity.showToast("抱歉，当前设备不支持对讲功能");
            return false;
        }
        switch (event.getAction()) {
            // 按下开启对讲
            case MotionEvent.ACTION_DOWN: {
                mParentActivity.mActivePlayer.closeSound();
                mParentActivity.mActiveTalker.setVoiceTalkStatus(true);
                break;
            }
            // 松开关闭对讲
            case MotionEvent.ACTION_UP: {
                mParentActivity.mActiveTalker.setVoiceTalkStatus(false);
                mParentActivity.mActivePlayer.openSound();
                break;
            }
        }
        return false;
    }
}
