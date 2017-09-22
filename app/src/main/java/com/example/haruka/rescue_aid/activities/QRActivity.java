package com.example.haruka.rescue_aid.activities;

/**
 * Created by skriulle on 9/16/2017 AD.
 */

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Toast;

import com.example.haruka.rescue_aid.utils.MedicalCertification;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.Reader;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

import java.util.List;

public class QRActivity extends AppCompatActivity {

    private SurfaceView mSurfaceView;
    private Camera mCamera;
    static final int REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSurfaceView = new SurfaceView(this);
        mSurfaceView.setOnClickListener(onClickListener);
        setContentView(mSurfaceView);
        addContentView(new CameraOverlayView(this), new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
    }

    @Override
    protected void onResume() {
        super.onResume();
        SurfaceHolder holder = mSurfaceView.getHolder();
        holder.addCallback(callback);
    }

    private SurfaceHolder.Callback callback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            int permissionCheck = ContextCompat.checkSelfPermission(QRActivity.this, Manifest.permission.CAMERA);

            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {

                if (ActivityCompat.shouldShowRequestPermissionRationale(QRActivity.this, Manifest.permission.CAMERA)) {
                }

            } else {
            }
            mCamera = Camera.open();
            mCamera.lock();
            mCamera.setDisplayOrientation(90);
            try {
                mCamera.setPreviewDisplay(holder);
            } catch (Exception e) {
                e.printStackTrace();
            }

/*
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setColor(Color.BLUE);
            paint.setStyle(Paint.Style.FILL);

            Canvas canvas = holder.lockCanvas();
            //canvas.drawColor(Color.BLACK);
            canvas.drawCircle(100, 200, 50, paint);
            holder.unlockCanvasAndPost(canvas);
*/

        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            Camera.Parameters parameters = mCamera.getParameters();
            List<Camera.Size> previewSizes = parameters.getSupportedPreviewSizes();
            Camera.Size previewSize = previewSizes.get(0);
            parameters.setPreviewSize(previewSize.width, previewSize.height);
            mCamera.setParameters(parameters);
            mCamera.startPreview();
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            mCamera.release();
            mCamera = null;
        }
    };

    private OnClickListener onClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mCamera != null) {
                mCamera.autoFocus(autoFocusCallback);
            }
        }
    };

    private AutoFocusCallback autoFocusCallback = new AutoFocusCallback() {
        @Override
        public void onAutoFocus(boolean success, Camera camera) {
            if (success) {
                camera.setOneShotPreviewCallback(previewCallback);
            }
        }
    };

    private void showQR(MedicalCertification medicalCertification) {
        final Intent QRIntent = new Intent(this, QRDisplayActivity.class);

        QRIntent.putExtra("RESULT", medicalCertification.toString());
        Log.d("RESULT", medicalCertification.toString());
        startActivity(QRIntent);
    }

    private void showCertification(MedicalCertification medicalCertification) {
        final Intent QRIntent = new Intent(this, CertificationActivity.class);

        QRIntent.putExtra("CERTIFICATION", medicalCertification);
        Log.d("RESULT", medicalCertification.toString());
        startActivity(QRIntent);
    }

    private Camera.PreviewCallback previewCallback = new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            int previewWidth = camera.getParameters().getPreviewSize().width;
            int previewHeight = camera.getParameters().getPreviewSize().height;

            PlanarYUVLuminanceSource source = new PlanarYUVLuminanceSource(
                    data, previewWidth, previewHeight, 0, 0, previewWidth, previewHeight, false);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

            Reader reader = new MultiFormatReader();
            Result result = null;
            try {
                result = reader.decode(bitmap);
                String text = result.getText();
                Toast.makeText(getApplicationContext(), text, Toast.LENGTH_LONG).show();
                Log.d("QR", text);
                MedicalCertification medicalCertification = new MedicalCertification(text);
                Log.i("medical certification", "read");
                medicalCertification.showRecords();
                //showQR(medicalCertification);
                showCertification(medicalCertification);
            } catch (Exception e) {
                Log.e("QR reader", e.toString());
                Toast.makeText(getApplicationContext(), "Not Found", Toast.LENGTH_SHORT).show();
            }
        }
    };

    class CameraOverlayView extends View {
        private int width, height;

        public CameraOverlayView(Context context) {
            super(context);
            setFocusable(true);
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            // ビューのサイズを取得
            width = w;
            height = h;
        }

        /**
         * 描画処理
         */
        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            // 背景色を設定
            canvas.drawColor(Color.TRANSPARENT);

            // 描画するための線の色を設定
            Paint paint = new Paint();
            paint.setStyle(Paint.Style.FILL);
            paint.setARGB(100, 0, 0, 0);

            // 上枠表示
            canvas.drawRect(0, 0, (width - height) / 2, height, paint);
            // 下枠表示
            canvas.drawRect((width - height) / 2 + height, 0, width, height, paint);

            // 円表示
            paint.setARGB(255, 255, 0, 0);
            paint.setStyle(Paint.Style.STROKE);
            canvas.drawRect(width / 14, height / 2-width/7*3,width / 14*13, height / 2+width/7*3, paint);
            paint = new Paint();
            paint.setARGB(128, 255, 255, 255);
            paint.setStyle(Paint.Style.FILL_AND_STROKE);
            canvas.drawRect(0, height / 2-width/7*3,width / 14, height / 2+width/7*3, paint);
            canvas.drawRect(width / 14*13, height / 2-width/7*3,width, height / 2+width/7*3, paint);
            canvas.drawRect(0,0,width,  height / 2-width/7*3, paint);
            canvas.drawRect(0,height / 2+width/7*3,width,  height, paint);
            paint = new Paint();
            paint.setStyle(Paint.Style.FILL_AND_STROKE);
            paint.setARGB(255, 255, 0, 0);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(30);
            canvas.drawText("QRコードを真ん中に合わせてタップしてください", width/2, height/2+width/7*3+30, paint);
        }

    }
}