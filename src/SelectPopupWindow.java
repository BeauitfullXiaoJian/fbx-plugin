package com.fbx;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.w3c.dom.Text;

import com.dobay.dudao.R;

public class SelectPopupWindow extends PopupWindow {
    public static final String TAG = "PopupWindowLog";

    private static final Integer FORM_CODE = 0;

    // 窗口视图对象
    private View mPopupView;

    // 父Activity对象
    private LiveActivity mParentActivity;

    // 父弹窗对象
    private CutPopupWindow mParentPopupWindow;

    // 浏览器组件
    private WebView mWebView;

    // 加载动画组件
    private ProgressBar mProgressBar;

    SelectPopupWindow(LiveActivity context, CutPopupWindow parentPopupWindow) {
        super(context);
        mParentActivity = context;
        mParentPopupWindow = parentPopupWindow;
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mPopupView = inflater.inflate(R.layout.popup_select_modal, null);
        setContentView(mPopupView);
        setWidth(ViewGroup.LayoutParams.MATCH_PARENT);
        setHeight(ViewGroup.LayoutParams.MATCH_PARENT);
        setFocusable(true);
        setAnimationStyle(R.style.popmenu_animation);
        setBackgroundDrawable(new ColorDrawable(0xb0000000));
        findView();
        initView();
    }

    private void findView() {
        mWebView = (WebView) mPopupView.findViewById(R.id.web_view);
        mProgressBar = (ProgressBar) mPopupView.findViewById(R.id.load_bar);
    }

    private void initView() {
        WebSettings webSettings = mWebView.getSettings();
        webSettings.setAllowUniversalAccessFromFileURLs(true);
        webSettings.setAllowFileAccessFromFileURLs(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setJavaScriptEnabled(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(false);
        mWebView.setWebChromeClient(new WebChromeClient() {

            @Override
            public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
                Log.d(TAG, "接收到参数" + message);
                result.cancel();
                SelectPopupWindow.this.dismiss();
                mParentPopupWindow.mSelectOptionsStr = message;
                mParentPopupWindow.updateSelectOptions();
                return true;
            }

            @Override
            public boolean onJsConfirm(WebView view, String url, String message, JsResult result) {
                result.cancel();
                SelectPopupWindow.this.dismiss();
                return true;
            }

            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
                if (newProgress >= 100) {
                    mProgressBar.setVisibility(View.INVISIBLE);
                }
            }
        });
        Log.d(TAG, LiveActivity.mApiData);
        mWebView.loadUrl("file:///android_asset/www/dist/index.html?" + LiveActivity.mApiData);
        if (mParentPopupWindow.mSelectOptionsStr == null) {
            Log.d(TAG,"清空缓存");
            WebStorage.getInstance().deleteOrigin("file:///android_asset/www/dist/index.html");
        }
    }
}
