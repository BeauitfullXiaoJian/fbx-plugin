package com.fbx;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;

import com.videogo.errorlayer.ErrorInfo;
import com.videogo.exception.BaseException;
import com.videogo.openapi.EZConstants;
import com.videogo.openapi.EZOpenSDK;

import com.dobay.dudao.R;

public class ControlPopupWindow extends PopupWindow implements View.OnTouchListener {

    // 窗口视图对象
    private View mPopupView;

    // 父Activity对象
    private LiveActivity mParentActivity;

    // 控制指令
    EZConstants.EZPTZCommand mPtzCommand;

    // 控制行为
    EZConstants.EZPTZAction mPtzAction;

    ControlPopupWindow(LiveActivity context) {
        super(context);
        mParentActivity = context;
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mPopupView = inflater.inflate(R.layout.popup_center_control, null);
        setContentView(mPopupView);
        setWidth(ViewGroup.LayoutParams.MATCH_PARENT);
        setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        setFocusable(true);
        setBackgroundDrawable(new ColorDrawable(0));
        setAnimationStyle(R.style.popmenu_animation);
        initEvent();
    }


    private void initEvent() {
        mPopupView.findViewById(R.id.btn_up).setOnTouchListener(this);
        mPopupView.findViewById(R.id.btn_left).setOnTouchListener(this);
        mPopupView.findViewById(R.id.btn_right).setOnTouchListener(this);
        mPopupView.findViewById(R.id.btn_down).setOnTouchListener(this);
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        switch (motionEvent.getAction()) {
            // 按下开始控制
            case MotionEvent.ACTION_DOWN: {
                switch (view.getId()) {
                    case R.id.btn_up:
                        mPtzCommand = EZConstants.EZPTZCommand.EZPTZCommandUp;
                        break;
                    case R.id.btn_left:
                        mPtzCommand = EZConstants.EZPTZCommand.EZPTZCommandLeft;
                        break;
                    case R.id.btn_right:
                        mPtzCommand = EZConstants.EZPTZCommand.EZPTZCommandRight;
                        break;
                    case R.id.btn_down:
                        mPtzCommand = EZConstants.EZPTZCommand.EZPTZCommandDown;
                        break;
                }
                mPtzAction = EZConstants.EZPTZAction.EZPTZActionSTART;
                break;
            }
            // 松开停止控制
            case MotionEvent.ACTION_UP: {
                mPtzAction = EZConstants.EZPTZAction.EZPTZActionSTOP;
                break;
            }
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    CameraData camera = mParentActivity.mActiveCamera;
                    EZOpenSDK.getInstance().controlPTZ(camera.getCameraSns(), camera.getCameraNo(),
                            mPtzCommand, mPtzAction, 1);
                } catch (final BaseException e) {
                    mParentActivity.showToast(e.getMessage());
                }
            }
        }).start();
        return true;
    }
}
