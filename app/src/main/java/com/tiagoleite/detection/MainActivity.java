package com.tiagoleite.detection;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.*;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.OptionalDataException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private TensorFlowClassifier tfClassifier;
    private CameraDevice cameraDevice;
    private String cameraId;
    private CameraCaptureSession cps;
    private Size imageDimension;
    private ImageReader imageReader;
    private Handler backgroundHandler;
    private HandlerThread backgroundThread;
    private TextureView textureView;
    private CaptureRequest.Builder captureRequestBuilder;
    private TextView textImageLabel;
    private static final int FINAL_W = 1200, FINAL_H = 822;

    CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            cameraDevice = camera;
            createCameraPreview();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            cameraDevice.close();
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            cameraDevice.close();
            cameraDevice = null;
        }
    };

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    private static final int REQ_CAM_PERMISSION  = 200;

    static
    {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textureView = findViewById(R.id.texture_view);
        textImageLabel = findViewById(R.id.tv_label);

        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                openCamera();
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {

            }
        });


        loadModel();

        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getFrame();
            }
        });

        /*byte[] arrayImage = getPixelsArray();
        Classification cls = tfClassifier.recognize(arrayImage);
        String label = cls.getLabel();*/
        //Log.d("debug", label);


    }

    private void getFrame()
    {
        if (cameraDevice==null)
            return;
        CameraManager cameraManager = (CameraManager)getSystemService(Context.CAMERA_SERVICE);
        try
        {
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraDevice.getId());
            Size[] jpegSizes = null;
            jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                .getOutputSizes(ImageFormat.JPEG);
            int w = 640;
            int h = 480;
            if (jpegSizes != null && jpegSizes.length > 0)
            {
                w = jpegSizes[0].getWidth();
                h = jpegSizes[0].getHeight();
            }
            ImageReader reader = ImageReader.newInstance(w, h, ImageFormat.JPEG, 1);
            List<Surface> outputSurface = new ArrayList<>(2);
            outputSurface.add(reader.getSurface());
            outputSurface.add(new Surface(textureView.getSurfaceTexture()));

            final CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(reader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);

            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, rotation);

            ImageReader.OnImageAvailableListener readListener = new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    Image image = null;
                    try
                    {
                        image = reader.acquireLatestImage();
                        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                        byte [] bytes = new byte[buffer.capacity()];
                        buffer.get(bytes);

                        Matrix matrix = new Matrix();
                        matrix.postRotate(90);
                        BitmapFactory.Options opt = new BitmapFactory.Options();
                        opt.inScaled = false;
                        opt.inPremultiplied = false;
                        opt.inMutable = true;

                        Bitmap bm = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, opt);

                        bm = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), matrix, true);

                        /*bm = Bitmap.createScaledBitmap(bm, bm.getWidth()/2, bm.getHeight()/2,
                                true);*/

                        byte[] arrayImage = getPixelsArray(bm);

                        Classification cls = tfClassifier.recognize(arrayImage, bm.getWidth(), bm.getHeight());
                        final String label = cls.getLabel() + " " + cls.getConf();
                        float[] box = cls.getBox();
                        Log.d("debug", "Res:" + label + " "+ cls.getConf());

                        Canvas tempCanvas = new Canvas(bm);
                        tempCanvas.drawBitmap(bm, 0, 0, null);
                        Paint paint = new Paint();
                        paint.setColor(Color.TRANSPARENT);
                        paint.setStyle(Paint.Style.FILL);

                        // FILL
                        /*tempCanvas.drawRect(box[1]*(float)bm.getWidth(),
                                box[0]*(float)bm.getHeight(),
                                box[3]*(float)bm.getWidth(),
                                box[0]*(float)bm.getHeight(), paint);*/

                        paint.setStrokeWidth(10);
                        paint.setColor(Color.RED);
                        paint.setStyle(Paint.Style.STROKE);
                        // BORDER
                        tempCanvas.drawRect(box[1]*(float)bm.getWidth(),
                                box[0]*(float)bm.getHeight(),
                                box[3]*(float)bm.getWidth(),
                                box[2]*(float)bm.getHeight(), paint);

                        final Bitmap ff = bm;
                        runOnUiThread(new Runnable() {

                            @Override
                            public void run() {
                                ImageView imageView = findViewById(R.id.image_view);
                                imageView.setImageBitmap(ff);
                                textImageLabel.setText(label);
                            }
                        });
                        //save(bytes);
                    }
                    catch (Exception e)
                    {
                        Log.d("debug", e.getMessage() + e.getClass());
                    }
                    finally {
                        if (image != null)
                            image.close();
                    }
                }
            };
            reader.setOnImageAvailableListener(readListener, backgroundHandler);

            final CameraCaptureSession.CaptureCallback captureListener =
                    new CameraCaptureSession.CaptureCallback() {
                        @Override
                        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                            super.onCaptureCompleted(session, request, result);
                            createCameraPreview();
                        }
                    };

            cameraDevice.createCaptureSession(outputSurface, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    try
                    {
                        session.capture(captureBuilder.build(), captureListener, backgroundHandler);
                    }
                    catch (Exception e)
                    {
                        Log.d("debug", e.getMessage() + e.getClass());
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {

                }
            }, backgroundHandler);

                

        }
        catch (Exception e)
        {

        }
    }

    private void createCameraPreview()
    {
        try
        {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            texture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight());
            Surface surface = new Surface(texture);
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);
            cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    if (cameraDevice != null)
                    {
                        cps = session;
                        updatePreview();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {

                }
            }, null);
        }
        catch (Exception e)
        {

        }
    }

    private void updatePreview()
    {
        if (cameraDevice==null)
        {
            Log.d("debug", "Error updating image");
        }
        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);
        try
        {
            cps.setRepeatingRequest(captureRequestBuilder.build(), null, backgroundHandler);

        }
        catch (Exception e)
        {
            Log.d("debug", e.getMessage()+e.getClass());
        }
    }

    private void openCamera()
    {
        CameraManager manager = (CameraManager)getSystemService(CAMERA_SERVICE);
        try
        {
            cameraId = manager.getCameraIdList()[0];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            imageDimension = map.getOutputSizes(SurfaceTexture.class)[0];

            if(ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED)
            {
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.CAMERA
                }, REQ_CAM_PERMISSION);
                return;
            }
            manager.openCamera(cameraId, stateCallback, null);

        }
        catch (Exception e)
        {

        }
    }

    public byte[] getPixelsArray()
    {
        Bitmap bm =  BitmapFactory.decodeResource(getResources(),
                R.drawable.alanis);
        bm = Bitmap.createScaledBitmap(bm, 320, 320, true);
        int h = bm.getHeight();
        int w = bm.getWidth();
        int pixels[] = new int[h*w];
        byte[] pixelsRet = new byte[h*w*3];
        bm.getPixels(pixels, 0, w, 0, 0, w, h);

        for (int i = 0; i < h; i++)
            for (int j = 0; j < w; j++)
            {
                int redValue = Color.red(pixels[i * w + j]);
                int blueValue = Color.blue(pixels[i * w + j]);
                int greenValue = Color.green(pixels[i * w + j]);
                pixelsRet[i * w + j] = (byte)redValue;
                pixelsRet[i * w + j + h*w] = (byte)greenValue;
                pixelsRet[i * w + j + 2*h*w] = (byte)blueValue;
            }

        return pixelsRet;
    }

    public byte[] getPixelsArray(Bitmap bm)
    {
        int h = bm.getHeight();
        int w = bm.getWidth();
        int pixels[] = new int[h*w];
        byte[] pixelsRet = new byte[h*w*3];
        bm.getPixels(pixels, 0, w, 0, 0, w, h);

        int cont=0;

        for (int i = 0; i < h; i++)
            for (int j = 0; j < w; j++)
            {
                int redValue = Color.red(pixels[i * w + j]);
                int blueValue = Color.blue(pixels[i * w + j]);
                int greenValue = Color.green(pixels[i * w + j]);
                pixelsRet[cont++] = (byte)redValue ;
                pixelsRet[cont++] = (byte)greenValue;
                pixelsRet[cont++] = (byte)blueValue;
            }

        return pixelsRet;
    }

    private void loadModel()
    {
        String[] tensorNames = new String[]{"detection_classes:0", "detection_boxes:0", "detection_scores:0"};
        try
        {
            tfClassifier = TensorFlowClassifier.create(getAssets(), "TensorFlow",
                    "frozen_inference_graph.pb",
                    "labels.txt",
                    "image_tensor:0",
                    tensorNames,
                    true);
        }
        catch (IOException e)
        {
            throw new RuntimeException("Error initializing classifiers!", e);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQ_CAM_PERMISSION)
        {
            if(grantResults[0] != PackageManager.PERMISSION_GRANTED )
            {
                Toast.makeText(this, "Libere a camera!", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        startBackgroundThread();
        if (textureView.isAvailable())
            openCamera();
        else
            textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
                @Override
                public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                    openCamera();
                }

                @Override
                public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

                }

                @Override
                public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                    return false;
                }

                @Override
                public void onSurfaceTextureUpdated(SurfaceTexture surface) {

                }
            });

    }

    @Override
    protected void onPause()
    {
        stopBackgroundTread();
        super.onPause();
    }

    private void stopBackgroundTread() {
        backgroundThread.quitSafely();
        try
        {
            backgroundThread.join();
            backgroundThread = null;
            backgroundHandler = null;
        }
        catch (Exception e)
        {

        }
    }

    private void startBackgroundThread()
    {
        backgroundThread = new HandlerThread("Camera BG");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }
}






