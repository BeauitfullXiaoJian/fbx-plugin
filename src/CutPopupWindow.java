package com.fbx;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.squareup.picasso.Picasso;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;

import com.dobay.dudao.MainActivity;
import com.dobay.dudao.R;

import okhttp3.Call;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class CutPopupWindow extends PopupWindow implements View.OnClickListener, okhttp3.Callback,
        TextWatcher {

    public static final String TAG = "PopupWindowLog";

    private static final Integer FORM_CODE = 0;

    // 窗口视图对象
    private View mPopupView;

    // 父Activity对象
    private LiveActivity mParentActivity;

    // 截图展示视图
    private ImageView mImageView;

    // 截图位图对象
    private Bitmap mPicture;

    // 选中的数据
    String mSelectOptionsStr;

    // 加载动画
    private View mLoadingView;

    private TextView mCameraTitleView;
    private TextView mSelectOptionsView;
    private TextView mTimeView;
    private EditText mContentView;
    private TextView mTotalView;
    private int shopId;

    CutPopupWindow(LiveActivity context, Bitmap picture, int shopId) {
        super(context);
        this.shopId = shopId;
        mParentActivity = context;
        mPicture = picture;
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mPopupView = inflater.inflate(R.layout.popup_cut_modal, null);
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
        mImageView = mPopupView.findViewById(R.id.picture_view);
        mCameraTitleView = (TextView) mPopupView.findViewById(R.id.camera_title);
        mTimeView = (TextView) mPopupView.findViewById(R.id.camera_time);
        mSelectOptionsView = (TextView) mPopupView.findViewById(R.id.select_text);
        mContentView = (EditText) mPopupView.findViewById(R.id.edit_content);
        mLoadingView = mPopupView.findViewById(R.id.loading_bar);
        mTotalView = mPopupView.findViewById(R.id.text_total);
        mPopupView.findViewById(R.id.submit_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                submitForm();
            }
        });
    }

    private void initView() {
        mImageView.setImageBitmap(mPicture);
        mImageView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                Drawable picture = mImageView.getDrawable();
                float wh = (float) (picture.getIntrinsicWidth()) / picture.getIntrinsicHeight();
                ViewGroup.LayoutParams params = mImageView.getLayoutParams();
                params.height = (int) (mImageView.getWidth() / wh);
                mImageView.setLayoutParams(params);
                return true;
            }
        });
        mPopupView.findViewById(R.id.select_btn).setOnClickListener(CutPopupWindow.this);
        mPopupView.findViewById(R.id.btn_close).setOnClickListener(CutPopupWindow.this);
        mImageView.setOnClickListener(CutPopupWindow.this);
        mContentView.addTextChangedListener(CutPopupWindow.this);
        mCameraTitleView.setText(mParentActivity.mActiveCamera.getCameraTitle());
        mTimeView.setText(timeString());
        updateSelectOptions();
    }

    void updateSelectOptions() {
        mSelectOptionsView.setText(mSelectOptionsStr == null ? "未选择" : "已选择");
    }

    @Override
    public void afterTextChanged(Editable editable) {
        mTotalView.setText(editable.length() + "/250");
    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.select_btn:
                new SelectPopupWindow(mParentActivity, CutPopupWindow.this, this.shopId)
                        .showAtLocation(mParentActivity.findViewById(R.id.main_view),
                                Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);
                break;
            case R.id.btn_close:
                dismiss();
                break;
            case R.id.picture_view:
                new DrawPicturePopupWindow(mParentActivity, CutPopupWindow.this, mPicture)
                        .showAtLocation(mParentActivity.findViewById(R.id.main_view),
                                Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);
                break;
        }
    }

    private void submitForm() {
        Log.d(TAG,String.valueOf(mSelectOptionsStr==null)); 
        // 上传图片
        try {
            String savePath = mParentActivity.getFilesDir().getAbsolutePath();
            String filePath = savePath + "/" + System.currentTimeMillis() + ".jpg";
            Log.d(TAG, "快照保存路径:" + filePath);
            File file = new File(filePath);
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
            mPicture.compress(Bitmap.CompressFormat.JPEG, 100, bos);
            bos.flush();
            bos.close();
            JsonObject authObject = ApiResponse.getAuthHeader(LiveActivity.mApiData);
            OkHttpClient client = new OkHttpClient();
            MultipartBody multipartBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("pic", file.getName(),
                            RequestBody.create(MultipartBody.FORM, file))
                    .build();
            Request request = new Request.Builder()
                    .addHeader("ng-params-one", authObject.get("ng-params-one").getAsString())
                    .addHeader("ng-params-two", authObject.get("ng-params-two").getAsString())
                    .addHeader("ng-params-three", authObject.get("ng-params-three").getAsString())
                    .url(LiveActivity.mCutImageUrl)
                    .post(multipartBody)
                    .build();
            Call call = client.newCall(request);
            call.enqueue(CutPopupWindow.this);
        } catch (IOException e) {
            e.printStackTrace();
        }        
            mLoadingView.setVisibility(View.VISIBLE);    
    }

    @Override
    public void onResponse(Call call, Response response) {
        mParentActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mLoadingView.setVisibility(View.INVISIBLE);
            }
        });
        Log.d(TAG, "请求回调成功");
        ResponseBody responseBody = response.body();
        if (responseBody != null) {
            try {
                String apiString = responseBody.string();
                i(TAG, apiString);
                ApiResponse apiResponse = new ApiResponse(apiString);
                if (apiResponse.getResult()) {
                    mParentActivity.showToast("已保存至考评库");
                    String imgUrl = apiResponse.getData().getAsString();
                    Log.d("TEST",imgUrl);
                    // 存储localStorage
                    MainActivity.getMainActivity().loadJs("window.nativeCallJs.setCheckStorage('"
                        +mSelectOptionsStr+"','"+mContentView.getText().toString()
                        +"','"+imgUrl+"',"+mParentActivity.mStoreData.getStoreId()+",'"
                        +mParentActivity.mStoreData.getStoreTitle()+"','"
                        +mParentActivity.mActiveCamera.getCameraTitle()+"',"+LiveActivity.mTermPlanId+","
                        +LiveActivity.mTermId+",'"+LiveActivity.mTermName+"')");
                    mParentActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dismiss();
                        }
                    });
                }else{
                    mParentActivity.showToast(apiResponse.getMessage());
                }
            } catch (Exception e) {
                e.printStackTrace();
                mParentActivity.showToast("服务器异常");
            }
        }
    }

    @Override
    public void onFailure(Call call, IOException e) {
        mParentActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mLoadingView.setVisibility(View.INVISIBLE);
            }
        });
        Log.d(TAG, "请求失败");
        mParentActivity.showToast("数据通信异常，请检查您的网络");
    }

    public void updatePicture(Bitmap picture){
        mPicture = picture;
        mImageView.setImageBitmap(mPicture);
    }

    public static void i(String tag, String msg) {
        int max_str_length = 2001 - tag.length();
        while (msg.length() > max_str_length) {
            Log.i(tag, msg.substring(0, max_str_length));
            msg = msg.substring(max_str_length);
        }
        Log.i(tag, msg);
    }

    private String timeString() {
        final Calendar calendar = Calendar.getInstance();
        String[] weeks = {"天", "一", "二", "三", "四", "五", "六"};
        return calendar.get(Calendar.YEAR) + "."
                + calendar.get(Calendar.MONTH) + "."
                + calendar.get(Calendar.DAY_OF_MONTH) + " 星期"
                + weeks[calendar.get(Calendar.DAY_OF_WEEK) - 1] + " "
                + timeNum((calendar.get(Calendar.HOUR_OF_DAY))) + ":"
                + timeNum(calendar.get(Calendar.MINUTE)) + ":"
                + timeNum(calendar.get(Calendar.SECOND));
    }

    private String timeNum(int timeNum) {
        return (timeNum < 10 ? "0" : "") + timeNum;
    }
}