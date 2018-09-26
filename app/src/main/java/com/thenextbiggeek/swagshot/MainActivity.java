package com.thenextbiggeek.swagshot;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
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
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    String logTag = "swagshotLog";

    TextureView mTextureView;
    ImageView mCaptureButton;

    private Size imageDimension;
    File file;
    int surfaceWidth, surfaceHeight, displayWidth, displayHeight;
    int newHeight, newWidth;


    private String cameraId;
    protected CameraDevice cameraDevice;
    private String TAG = "CameraMainActivity";
    private CaptureRequest.Builder captureRequestBuilder;
    private CaptureRequest captureRequest;
    private CameraCaptureSession cameraCaptureSessions;
    private Handler mBackgroundHandler;
    private HandlerThread mHandlerThread;
    private boolean mFlashSupported;
    private ImageReader imageReader;
    private Context mContext;
    int camNo =0;

    Bundle mBundle;


    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private int PICK_IMAGE = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //actionbar for loserz
        if(requestWindowFeature(Window.FEATURE_NO_TITLE)){
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        mBundle = savedInstanceState;
        setContentView(R.layout.activity_main);

        mContext = getApplicationContext();
        getDisplayWidthHeight();
        mTextureView = findViewById(R.id.cameraTextureView);
        mCaptureButton = findViewById(R.id.captureButton);
        mCaptureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePicture();
            }
        });
        //TODO permissions. assumed that its' done
    }

    private void getDisplayWidthHeight() {
        DisplayMetrics mDisplayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(mDisplayMetrics);
        displayHeight = mDisplayMetrics.heightPixels;
        displayWidth = mDisplayMetrics.widthPixels;
    }

    TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            //when the texture is available, the camera can be opened

            openCamera();
            surfaceWidth = width;
            surfaceHeight = height;
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
    };
    private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            Log.e(logTag, "the camera is opened");
            cameraDevice = camera;
            createCameraPreview();

        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            //closing cameraDevice
            cameraDevice.close();
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            cameraDevice.close();
            cameraDevice = null;
            Log.e(logTag, "error on opening cameraDevice");
        }
    };

    private void createCameraPreview() {
        try{
            SurfaceTexture mTexture = mTextureView.getSurfaceTexture();
            mTexture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight());
            Surface surface = new Surface(mTexture);
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);
            cameraDevice.createCaptureSession(Arrays.asList(surface), new
                    CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    if(null == cameraDevice){
                        return;
                    }
                    //cameraDevice is not null
                    cameraCaptureSessions = session;
                    //now that we have started our preview, lets update it
                    updatePreview();
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                }
            }, null);
        }catch (CameraAccessException e){
            e.printStackTrace();

        }

    }

    private void updatePreview() {
        if (cameraDevice != null){
            captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
            try{
                cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(),
                        null, mBackgroundHandler);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    private void openCamera() {
        CameraManager mManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        Log.e(TAG, "is camera Open?");
        try {
            cameraId = mManager.getCameraIdList()[camNo];
            CameraCharacteristics characteristics = mManager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;
            Size optimalSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
             displayWidth, displayHeight);
            //imageDimension = map.getOutputSizes(SurfaceTexture.class)[11];
            imageDimension = optimalSize;
            setAspectRatioTextureView(mTextureView, imageDimension.getWidth(), imageDimension.getHeight());

            //TODO Add permission for camera and let user grant the permission
            //and after permission is dealt with,
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            mManager.openCamera(cameraId, mStateCallback, null);
            Log.e(TAG, "cameraOpened in openCamera");

        }catch (CameraAccessException e){
            Log.e(TAG, e.getMessage());
        }

    }

    /**
     * params
     * Size[] - the array of sizes possible through the camera
     * int width = displayWidth; int height = displayHeight
     */
    public Size chooseOptimalSize(Size[] choices, int width, int height){
        double aspectRatio = (double) height/width; //since in portrait height > width
        double aspectRatioArray[] = new double[50];
        Log.e("DISPLAY", " = Dw" + displayWidth + " = Dh" +displayHeight + '\n');

        int i = 0;
        for(Size opt: choices){
            int optWidth = opt.getWidth();
            int optHeight = opt.getHeight();

            Log.e("Tag", i + " = w" + optWidth + " = h" +optHeight + '\n');
            if(optWidth > optHeight){
                aspectRatioArray[i] = (double) optWidth/optHeight;
                i++;
            }else{
                aspectRatioArray[i] = (double) optWidth/optHeight;
                i++;
            }

        }
        int indexNumber = findClosestNumber(aspectRatioArray,aspectRatio);
        return choices[indexNumber];



    }


    public int findClosestNumber(double[] sizeAspectRatios, double aspectRatio){
        double myNumber = aspectRatio;
        double distance = Math.abs(sizeAspectRatios[0] - myNumber);
        int idx = 0;
        for(int c = 1; c < sizeAspectRatios.length; c++){
            double cdistance = Math.abs(sizeAspectRatios[c] - myNumber);
            if(cdistance < distance){
                idx = c;
                distance = cdistance;
            }
        }
        Log.e("IDX", String.valueOf(idx));
        return idx;
    }



    public double roundOff(double num, int noAfterDecimal){
        BigDecimal bigDecimal = new BigDecimal(num);
        bigDecimal = bigDecimal.setScale(noAfterDecimal, BigDecimal.ROUND_HALF_UP);
        return bigDecimal.doubleValue();
    }



    private void setAspectRatioTextureView(TextureView textureView, int width, int height) {

        Matrix mMatrix = new Matrix();
        mMatrix.setScale((float) 1.2,(float) 1.2);
        if(width > height){
             newWidth = displayWidth;
             newHeight = ((displayWidth * width)/height);
            Log.d(TAG, "TextureView Width : " + newWidth + " TextureView Height : " + newHeight);
            //textureView.setTransform(mMatrix);
            textureView.setLayoutParams(new FrameLayout.LayoutParams(newWidth, newHeight));

            //textureView.setLayoutParams(new FrameLayout.LayoutParams(displayWidth, displayHeight));

        }else {
             newWidth = displayWidth;
             newHeight = ((displayWidth * height)/width);
            Log.d(TAG, "TextureView Width : " + newWidth + " TextureView Height : " + newHeight);
            //textureView.setTransform(mMatrix);
            textureView.setLayoutParams(new FrameLayout.LayoutParams(newWidth, newHeight));


            // textureView.setLayoutParams(new FrameLayout.LayoutParams(displayWidth, displayHeight));

        }

    }

    protected void takePicture() {
        if(null == cameraDevice) {
            Log.e(TAG, "cameraDevice is null");
            return;
        }
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraDevice.getId());
            Size[] jpegSizes = null;
            if (characteristics != null) {
                jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG);
            }
            int width = 640;
            int height = 480;
            if (jpegSizes != null && 0 < jpegSizes.length) {
                width = jpegSizes[0].getWidth();
                height = jpegSizes[0].getHeight();
            }
            ImageReader reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);
            List<Surface> outputSurfaces = new ArrayList<Surface>(2);
            outputSurfaces.add(reader.getSurface());
            outputSurfaces.add(new Surface(mTextureView.getSurfaceTexture()));
            final CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(reader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
            // Orientation
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));
            SimpleDateFormat s = new SimpleDateFormat("ddMMyyyyhhmmss");
            String timestamp = s.format(new Date());
            final File file = new File(Environment.getExternalStorageDirectory()+"/pic"+timestamp+".jpg");
            final Uri[] imageFileUri = new Uri[1];
            ImageReader.OnImageAvailableListener readerListener = new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    Image image = null;
                    try {
                        image = reader.acquireLatestImage();
                        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                        byte[] bytes = new byte[buffer.capacity()];
                        buffer.get(bytes);
                        save(bytes);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        if (image != null) {
                            image.close();
                        }
                    }
                }
                private void save(byte[] bytes) throws IOException {
                    OutputStream output = null;
                    try {
                        output = new FileOutputStream(file);
                        output.write(bytes);
                         imageFileUri[0] = Uri.fromFile(file);
                    } finally {
                        if (null != output) {
                            output.close();
                        }
                    }
                }
            };
            reader.setOnImageAvailableListener(readerListener, mBackgroundHandler);
            final CameraCaptureSession.CaptureCallback captureListener = new
                    CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request,
                                               TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    Toast.makeText(MainActivity.this, "Saved:" + file, Toast.LENGTH_SHORT).show();
                    createCameraPreview();

                   startEditorActivity(file);

                }
            };
            cameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    try {
                        session.capture(captureBuilder.build(), captureListener, mBackgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }
                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                }
            }, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void startEditorActivity(File file) {
        //the next activity opens
        Intent intent = new Intent(MainActivity.this, EditorActivity.class);
        intent.putExtra("URI", file.getPath() );
        startActivity(intent);
    }


    protected void startBackgroundThread(){
        mHandlerThread = new HandlerThread("Camera Background");
        mHandlerThread.start();
        mBackgroundHandler = new Handler(mHandlerThread.getLooper());

    }

    protected void stopBackgroundThread(){
        mHandlerThread.quitSafely();
        try{
            mHandlerThread.join();
            mHandlerThread = null;
            mBackgroundHandler = null;

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void closeCamera(){
        if(cameraDevice != null){
            cameraDevice.close();
            cameraDevice = null;
        }
        if(imageReader != null){
            imageReader.close();
            imageReader = null;
        }
    }


    @Override
    protected void onResume() {
        if(mBundle != null){
            camNo = mBundle.getInt("CAMERANO");
        }
        super.onResume();
        Log.e(TAG, "onResume");
        startBackgroundThread();
        if (mTextureView.isAvailable()) {
            openCamera();
        } else {
            mTextureView.setSurfaceTextureListener(textureListener);
        }
    }
    @Override
    protected void onPause() {
        Log.e(TAG, "onPause");
        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }

    FileOutputStream fileOutputStream;
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        if((requestCode == PICK_IMAGE) && (resultCode == RESULT_OK)){
            //make an inputStream and get the data into a file , pass the uri to the next activity
            //TODO MAKE TIMESTAMPS IN FINAL RELEASE GODDAMNIT
            Uri imageFromGalUri = data.getData();
            final File file = new File(Environment.getExternalStorageDirectory()+"/picfromGallery.jpg");
            Bitmap mBitmap = null;
            try {
                mBitmap  = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageFromGalUri);
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                fileOutputStream = new FileOutputStream(file);
                if (mBitmap != null) {
                    mBitmap.compress(Bitmap.CompressFormat.JPEG, 85, fileOutputStream);
                }
                fileOutputStream.flush();
                fileOutputStream.close();
            } catch (IOException e){
                e.printStackTrace();
            }

            startEditorActivity(file);


        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState){
        super.onSaveInstanceState(savedInstanceState);
        if(camNo == 0){
            savedInstanceState.putInt("CAMERANO", 1);
        }else if(camNo ==1){
            savedInstanceState.putInt("CAMERANO", 0);
        }
    }


    public void changeCamera(View view) {
        recreate();
    }


    public void galleryIntent(View view) {
        Intent galleryIntent = new Intent();
        galleryIntent.setType("image/*");
        galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(galleryIntent, PICK_IMAGE);
    }
}





