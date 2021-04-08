
package com.hiy.camerap;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
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
import androidx.core.app.ActivityCompat;

import com.alibaba.fastjson.JSON;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.FormatException;
import com.google.zxing.NotFoundException;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.oned.CodaBarReader;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends BaseAc {

    private Button mCaptureBtn;

    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    private Surface mSurface;
    private CameraDevice cameraDevice;
    CameraManager cameraManager;
    Size size = null;
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

        mBackThread = new HandlerThread("Thread-bg");
        mBackThread.start();


        View settingBtn = findViewById(R.id.setting_btn);
        settingBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, SettingAc.class);
                startActivity(intent);
            }
        });

        SurfaceHolder holder = mSurfaceView.getHolder();
        holder.addCallback(new SurfaceHolder.Callback() {
            @SuppressLint("MissingPermission")
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                Log.d(tag, "surfaceCreated");
                mSurfaceHolder = holder;
                initCamera();
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

//        requestCommodityInfo();
    }




    private void startCapture() {
        CaptureRequest.Builder builder;
        try {
            builder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            builder.addTarget(imageReader.getSurface());

            int angle = (getDisplayRotation() + 360) % 360;

            Log.d(tag, "角度" + getCameraOrientation() + "-" + getDisplayRotation() + '-' + angle);
            builder.set(CaptureRequest.JPEG_ORIENTATION, angle);

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
        if (cameraManager == null) {
            cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        }

        needCameraId = findCameraId();

        initImageReader();
    }

    private String findCameraId() {
        // camera 特征
//        CameraCharacteristics needCameraCharacteristics = null;
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

                if (characteristics.get(CameraCharacteristics.LENS_FACING) == null) {
                    continue;
                }
                int lens_facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                Log.d(tag, "" + lens_facing);
                if (lens_facing == CameraCharacteristics.LENS_FACING_BACK) {
//                    needCameraCharacteristics = characteristics;
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
                    cameraDevice = camera;
                    if (cameraDevice != null) {
                        cameraDevice.close();
                        cameraDevice = null;
                    }
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

        if (imageReader == null) {
            return;
        }

        try {
            List<Surface> surfaces = Arrays.asList(
                    mSurfaceHolder.getSurface(),
                    imageReader.getSurface()
            );
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

    private int getDisplayRotation() {
        int rotation = getWindowManager().getDefaultDisplay().getRotation();

        switch (rotation) {
            case Surface.ROTATION_0:
                return 90;
            case Surface.ROTATION_90:
                return 9;
            case Surface.ROTATION_180:
                return 270;
            case Surface.ROTATION_270:
                return 180;
        }
        return 0;
    }

    private int getCameraOrientation() {
        try {
            CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(needCameraId);
            return cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        return 0;
    }


    private void startPreView() {
        try {
            CaptureRequest.Builder builder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO);
            builder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);
            builder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, CameraMetadata.CONTROL_AE_PRECAPTURE_TRIGGER_START);

            builder.addTarget(mSurfaceHolder.getSurface());
//            builder.addTarget(imageReader.getSurface());

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

        imageReader = ImageReader.newInstance(1920, 1080, ImageFormat.JPEG, 2);
        imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                Log.d(tag, "onImageAvailable");
                Image image = reader.acquireLatestImage();
                if (image == null) {
                    return;
                }
                ImageUtils.saveImage(MainActivity.this, image);
//                Image.Plane[] planes = image.getPlanes();
//                ByteBuffer buffer = planes[0].getBuffer();
//                byte[] data = new byte[buffer.remaining()];
//                buffer.get(data);
//                Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
//                readBarCode(bitmap);
                image.close();


            }
        }, new Handler(mBackThread.getLooper()));
    }


    @SuppressLint("MissingPermission")
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onResume() {
        super.onResume();
        if (!checkPermission()) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, HiyConstant.S_REQUEST_CODE_CAMERA);
        } else {
            initCamera();
        }

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

    private void readBarCode(Bitmap bmp) {
        int width = bmp.getWidth();
        int height = bmp.getHeight();
        int[] pixels = new int[width * height];
        bmp.getPixels(pixels, 0, width, 0, 0, width, height);

        CodaBarReader reader = new CodaBarReader();
        Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
        hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);//优化精度
        hints.put(DecodeHintType.CHARACTER_SET, "utf-8");//解码设置编码方式为：utf-8
        HybridBinarizer binarizer = new HybridBinarizer(new RGBLuminanceSource(bmp.getWidth(), bmp.getHeight(), pixels));
        BinaryBitmap binaryBitmap = new BinaryBitmap(binarizer);
        try {
            Result result = reader.decode(binaryBitmap, hints);
            Log.d(tag, "barcode ==>" + result.getText());
        } catch (NotFoundException e) {
            e.printStackTrace();
        } catch (FormatException e) {
            e.printStackTrace();
        }
    }
}