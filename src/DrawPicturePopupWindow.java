package com.fbx;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.support.design.widget.FloatingActionButton;

import com.dobay.dudao.R;

import java.util.ArrayList;

public class DrawPicturePopupWindow extends PopupWindow implements View.OnClickListener {

    // 父弹窗对象
    private CutPopupWindow mParentPopupWindow;

    private static final String TAG = "DrawPictureLog";
    private Context mContext;
    private View mMainView;
    private RelativeLayout mMainLayout;
    private Bitmap mDrawImage;
    private DrawImageView mDrawImageView;

    DrawPicturePopupWindow(Activity context, CutPopupWindow parentPopupWindow, Bitmap picture) {
        super(context);
        mContext = context;
        mParentPopupWindow = parentPopupWindow;
        Matrix matrix = new Matrix();
        matrix.setRotate(90, picture.getWidth(), picture.getHeight());
        mDrawImage = Bitmap.createBitmap(picture, 0, 0, picture.getWidth(),
                picture.getHeight(), matrix, true);
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (inflater != null) {
            mMainView = inflater.inflate(R.layout.popup_image_draw, null);
            setContentView(mMainView);
            setWidth(ViewGroup.LayoutParams.MATCH_PARENT);
            setHeight(ViewGroup.LayoutParams.MATCH_PARENT);
            setFocusable(true);
            setAnimationStyle(R.style.popmenu_animation);
            setBackgroundDrawable(new ColorDrawable(0xb0000000));
            initView();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_save: {
                // 请求保存图片，需要获取写入权限
                Bitmap bitmap = mDrawImageView.getImageBitmap();
                mParentPopupWindow.updatePicture(bitmap);
                dismiss();
                break;
            }
            case R.id.btn_cancel: {
                // 撤销之前的操作
                mDrawImageView.reDo();
                break;
            }
            case R.id.btn_black: {
                changePaintColor(Color.BLACK);
                break;
            }
            case R.id.btn_red: {
                changePaintColor(Color.RED);
                break;
            }
            case R.id.btn_green: {
                changePaintColor(Color.GREEN);
                break;
            }
        }
    }

    private void initView() {
        // 获取主视图布局
        mMainLayout = mMainView.findViewById(R.id.image_draw_layout);
        // 初始化绘画板
        mDrawImageView = new DrawImageView(this.mContext);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
        layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
        mDrawImageView.setLayoutParams(layoutParams);
        mDrawImageView.setScaleType(ImageView.ScaleType.FIT_XY);
        mDrawImageView.setBackgroundColor(Color.BLACK);
        mMainLayout.addView(mDrawImageView, 0);
        // 设置画板图片
        ViewGroup.LayoutParams params = mDrawImageView.getLayoutParams();
        mDrawImageView.setLayoutParams(params);
        mDrawImageView.setImageBitmap(mDrawImage);
        Log.d(TAG, "图片设置成功");
        // 设置相关按钮事件
        mMainLayout.findViewById(R.id.btn_cancel).setOnClickListener(DrawPicturePopupWindow.this);
        mMainLayout.findViewById(R.id.btn_save).setOnClickListener(DrawPicturePopupWindow.this);
        mMainLayout.findViewById(R.id.btn_black).setOnClickListener(DrawPicturePopupWindow.this);
        mMainLayout.findViewById(R.id.btn_red).setOnClickListener(DrawPicturePopupWindow.this);
        mMainLayout.findViewById(R.id.btn_green).setOnClickListener(DrawPicturePopupWindow.this);
    }

    private void changePaintColor(int color) {
        ((FloatingActionButton) mMainLayout.findViewById(R.id.btn_black)).setImageDrawable(null);
        ((FloatingActionButton) mMainLayout.findViewById(R.id.btn_red)).setImageDrawable(null);
        ((FloatingActionButton) mMainLayout.findViewById(R.id.btn_green)).setImageDrawable(null);
        FloatingActionButton btn;
        if (color == Color.BLACK) {
            btn = mMainLayout.findViewById(R.id.btn_black);
        } else if (color == Color.RED) {
            btn = mMainLayout.findViewById(R.id.btn_red);
        } else {
            btn = mMainLayout.findViewById(R.id.btn_green);
        }
        Drawable icon = mContext.getResources().getDrawable(R.drawable.ic_check_black_24dp);
        btn.setImageDrawable(icon);
        mDrawImageView.setPaintColor(color);
    }

    public class DrawLine {
        ArrayList<Point> points;
        Paint paint;

        DrawLine(ArrayList<Point> drawPoints, Paint drawPaint) {
            this.points = drawPoints;
            this.paint = new Paint(drawPaint);
        }
    }

    public class DrawImageView extends android.support.v7.widget.AppCompatImageView {

        private DrawLine drawLine;

        private ArrayList<DrawLine> drawLines = new ArrayList<DrawLine>();

        private Paint drawPaint;

        public DrawImageView(Context context) {
            super(context);
            drawPaint = new Paint();
            drawPaint.setColor(Color.RED);
            drawPaint.setAntiAlias(true);
            drawPaint.setStrokeJoin(Paint.Join.ROUND);
            drawPaint.setStrokeCap(Paint.Cap.ROUND);
            drawPaint.setStrokeWidth(10);
            drawLine = new DrawLine(new ArrayList<Point>(), drawPaint);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            // 绘制线条
            for (DrawLine line : drawLines) {
                drawLine(canvas, line);
            }
            drawLine(canvas, drawLine);
            Log.d(TAG, "重绘成功");
        }

        @Override
        public boolean performClick() {
            return super.performClick();
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {

            // 获取坐标
            int x = (int) event.getX();
            int y = (int) event.getY();
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN: {
                    Log.d(TAG, "开始绘制线条");
                    performClick();
                    break;
                }
                case MotionEvent.ACTION_MOVE: {
                    Log.d(TAG, "正在绘制线条");
                    drawLine.points.add(new Point(x, y));
                    break;
                }
                case MotionEvent.ACTION_UP: {
                    Log.d(TAG, "结束绘制线条");
                    drawLines.add(drawLine);
                    drawLine = new DrawLine(new ArrayList<Point>(), drawPaint);
                    break;
                }
            }
            // 重绘视图
            invalidate();
            return true;
        }

        private void drawLine(Canvas canvas, DrawLine line) {
            for (int i = 0; i < line.points.size() - 1; i++) {
                Point startPoint = line.points.get(i);
                Point endPoint = line.points.get(i + 1);
                canvas.drawLine(startPoint.x, startPoint.y, endPoint.x, endPoint.y, line.paint);
            }
        }

        public void setPaintColor(int color) {
            drawPaint.setColor(color);
            drawLine.paint.setColor(color);
        }

        public void reDo() {
            int lineNum = this.drawLines.size();
            if (lineNum > 0) {
                this.drawLines.remove(lineNum - 1);
                invalidate();
            }
        }

        public Bitmap getImageBitmap() {
            this.setDrawingCacheEnabled(true);
            Bitmap bitmap = this.getDrawingCache();
            Matrix matrix = new Matrix();
            matrix.setRotate(-90, bitmap.getWidth(), bitmap.getHeight());
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
                    bitmap.getHeight(), matrix, true);
            return bitmap;
        }
    }
}
