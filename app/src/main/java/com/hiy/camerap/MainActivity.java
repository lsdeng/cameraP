
package com.hiy.camerap;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.alibaba.fastjson.JSON;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private static final String tag = "MainActivity";

    private Button mCaptureBtn;

    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    private Surface mSurface;
    private CameraDevice cameraDevice;
    CameraManager cameraManager;
    String needCameraId;
    ImageReader imageReader;
    CameraCaptureSession mCaptureSession;
    HandlerThread mBackThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSurfaceView = findViewById(R.id.surface);

        mCaptureBtn = findViewById(R.id.capture_btn);
        mCaptureBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //拍照
                startCapture();
            }
        });

        mBackThread = new HandlerThread("back");
        mBackThread.start();

        requestCommodityInfo();
    }


    public void requestCommodityInfo() {



        OkHttpClient client = new OkHttpClient.Builder().build();

        HttpUrl httpUrl = new HttpUrl.Builder()
                .scheme("http")
                .host("www.mxnzp.com")
                .addPathSegments("api/barcode/goods/details")
                .addQueryParameter("barcode", "6922266454295")
                .addQueryParameter("app_id", HiyConstant.S_BARCODE_APP_ID)
                .addQueryParameter("app_secret", HiyConstant.S_BARCODE_APP_SECRET)
                .build();

        Log.d(tag, httpUrl.toString());

        Request request = new Request.Builder().url(httpUrl.toString()).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.d(tag, "onFailure-" + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String resString = response.body().string();
                Log.d(tag, "response-" + resString);
            }
        });
    }

    private void startCapture() {
        CaptureRequest.Builder builder;
        try {
            builder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            builder.addTarget(imageReader.getSurface());

            mCaptureSession.stopRepeating();
            mCaptureSession.capture(builder.build(), new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    startCaptureSession();
                }
            }, null);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private boolean checkPermission() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }


    @SuppressLint("MissingPermission")
    private void initCamera() {
        if (!checkPermission()) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, HiyConstant.S_REQUEST_CODE_CAMERA);
            return;
        }

        if (cameraManager != null) {
            return;
        }
        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        needCameraId = findCameraId();
        initImageReader();

        SurfaceHolder holder = mSurfaceView.getHolder();
        holder.addCallback(new SurfaceHolder.Callback() {
            @SuppressLint("MissingPermission")
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                Log.d(tag, "surfaceCreated");
                mSurfaceHolder = holder;
                openCamera();
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                Log.d(tag, "surfaceChanged" + "[" + format + "," + width + "," + height + "]");
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                Log.d(tag, "surfaceDestroyed");
                release();
            }
        });
    }

    private String findCameraId() {
        // camera 特征
        CameraCharacteristics needCameraCharacteristics = null;
        try {
            String[] cameraIds = cameraManager.getCameraIdList();
            Log.d(tag, JSON.toJSONString(cameraIds));

            for (String cameraId : cameraIds) {
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
                StreamConfigurationMap streamConfigurationMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                Size[] sizes = streamConfigurationMap.getOutputSizes(ImageFormat.JPEG);

                for (Size size : sizes) {
                    Log.d(tag, "size =>" + "[" + size.getWidth() + "," + size.getHeight() + "]");
                }

                int lens_facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                Log.d(tag, "" + lens_facing);
                if (lens_facing == CameraCharacteristics.LENS_FACING_BACK) {
                    needCameraCharacteristics = characteristics;
                    return cameraId;
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        return null;
    }

    private void openCamera() {
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return;
            }

            if (needCameraId == null) {
                return;
            }

            Log.d(tag, "openCamera");
            cameraManager.openCamera(needCameraId, new CameraDevice.StateCallback() {
                @SuppressLint("MissingPermission")
                @RequiresApi(api = Build.VERSION_CODES.P)
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    cameraDevice = camera;
                    startCaptureSession();
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {
                    camera.close();
                }

                @Override
                public void onError(@NonNull CameraDevice camera, int error) {
                    camera.close();
                }
            }, new Handler(Looper.getMainLooper()));
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    private void startCaptureSession() {
        if (mSurfaceHolder == null) {
            return;
        }
        try {
            List<Surface> surfaces = Arrays.asList(mSurfaceHolder.getSurface(), imageReader.getSurface());
            cameraDevice.createCaptureSession(
                    surfaces,
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            mCaptureSession = session;
                            startPreView();
                        }


                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {

                        }
                    }, new Handler(Looper.getMainLooper())
            );


        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    private void startPreView() {
        try {
            CaptureRequest.Builder builder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            builder.addTarget(mSurfaceHolder.getSurface());

            CaptureRequest captureRequest = builder.build();
            mCaptureSession.setRepeatingRequest(captureRequest, new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, long timestamp, long frameNumber) {
                    super.onCaptureStarted(session, request, timestamp, frameNumber);
                }

                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                }


            }, new Handler(Looper.getMainLooper()));
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void initImageReader() {
        if (imageReader != null) {
            imageReader.close();
            imageReader = null;
        }
        imageReader = ImageReader.newInstance(720, 1280, ImageFormat.JPEG, 5);
        imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                Image image = reader.acquireLatestImage();
                ImageUtils.saveImage(MainActivity.this, image);
                Log.d(tag, "onImageAvailable");
                ThreadUtils.isMainThread();
            }
        }, new Handler(mBackThread.getLooper()));
    }


    @SuppressLint("MissingPermission")
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onResume() {
        super.onResume();
        initCamera();

    }

    @Override
    protected void onPause() {
        super.onPause();
        // todo
    }

    @Override
    protected void onStop() {
        super.onStop();
        release();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();


        if (mBackThread != null) {
            mBackThread.quitSafely();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == HiyConstant.S_REQUEST_CODE_CAMERA) {
            initCamera();
        }
    }

    private void release() {
        if (mCaptureSession != null) {
            mCaptureSession.close();
            mCaptureSession = null;
        }

        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }

        if (imageReader != null) {
            imageReader.close();
            imageReader = null;
        }
    }
}