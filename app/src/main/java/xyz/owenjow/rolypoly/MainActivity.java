package xyz.owenjow.rolypoly;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import static android.os.Environment.DIRECTORY_RINGTONES;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class MainActivity extends AppCompatActivity {

    public static Camera mCamera = null;
    private CameraPreview mCameraPreview = null;
    public static final int MEDIA_TYPE_IMAGE = 1;
    private int pictureWidth = -1, pictureHeight = -1;
    private Camera.Face[] identifiedFaces = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (ContextCompat.checkSelfPermission(this.getApplicationContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1);
        }

        if (ContextCompat.checkSelfPermission(this.getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }

        setContentView(R.layout.activity_main);

        try {
            releaseCamera();
            mCamera = Camera.open();//you can use open(int) to use different cameras
        } catch (Exception e) {
            Log.d("ERROR", "Failed to get camera: " + e.getMessage());
        }

        if (mCamera != null) {
            mCameraPreview = new CameraPreview(this, mCamera);//create a SurfaceView to show camera data
            FrameLayout camera_view = (FrameLayout) findViewById(R.id.camera_preview);
            camera_view.addView(mCameraPreview);//add the SurfaceView to the layout

            mCameraPreview.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {

                    if (mCamera != null) {
                        Camera camera = mCamera;
                        camera.cancelAutoFocus();

                        Camera.Parameters parameters = camera.getParameters();
                        if (parameters.getFocusMode() != Camera.Parameters.FOCUS_MODE_AUTO) {
                            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                        }
                        if (parameters.getMaxNumFocusAreas() > 0) {
                            ArrayList<Camera.Area> focusAreas = new ArrayList<Camera.Area>(1);
                            focusAreas.add(new Camera.Area(new Rect(-1000, -1000, 1000, 0), 750));
                            parameters.setFocusAreas(focusAreas);
                        }

                        try {
                            camera.cancelAutoFocus();
                            camera.setParameters(parameters);
                            camera.startPreview();
                            camera.autoFocus(new Camera.AutoFocusCallback() {
                                @Override
                                public void onAutoFocus(boolean success, Camera camera) {
                                    if (camera.getParameters().getFocusMode() != Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE) {
                                        Camera.Parameters parameters = camera.getParameters();
                                        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                                        if (parameters.getMaxNumFocusAreas() > 0) {
                                            parameters.setFocusAreas(null);
                                        }
                                        camera.setParameters(parameters);
                                        camera.startPreview();
                                    }
                                }
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    return true;
                }
            });
        }
    }

    public static final String TAG = "ROLYPOLY";
    public Camera CAMERA_INSTANCE = mCamera;

    public void takePicture(View view) {
        Intent intent = new Intent(this, CameraActivity.class);
        startActivity(intent);
    }

    public static int cameraId;
    public static boolean cameraFront;

    public static int findFrontFacingCamera() {

        // Search for the front facing camera
        int numberOfCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                cameraId = i;
                cameraFront = true;
                break;
            }
        }
        return cameraId;
    }

    public void releaseCamera() {
        if (mCamera != null) {
            mCamera.release();        // release the camera for other applications
            mCamera = null;
        }
    }

    public void goIdentify(View view) {
        System.out.println("started");
        mCamera.setFaceDetectionListener(faceDetectionListener);
        mCamera.startFaceDetection();
    }

    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            System.out.println(Arrays.toString(data));
            System.out.println(data.length);

            pictureWidth = camera.getParameters().getPictureSize().width;
            pictureHeight = camera.getParameters().getPictureSize().height;

            android.graphics.Matrix matrix = new android.graphics.Matrix();
            Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
            camera.getCameraInfo(cameraId, info);
            // Need mirror for front camera.
            boolean mirror = (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT);
            matrix.setScale(mirror ? -1 : 1, 1);
            // This is the value for android.hardware.Camera.setDisplayOrientation.
            matrix.postRotate(info.orientation);
            // Camera driver coordinates range from (-1000, -1000) to (1000, 1000).
            // UI coordinates range from (0, 0) to (width, height).
            matrix.postScale(pictureWidth / 2000f, pictureHeight / 2000f);
            matrix.postTranslate(pictureWidth / 2f, pictureHeight / 2f);

            Log.d("onPictureTaken", "saved file");
            File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
            if (pictureFile == null){
                Log.d(TAG, "Error creating media file, check storage permissions: ");
                return;
            }

            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data);
                fos.close();

                if(pictureFile.exists()){

                    Bitmap myBitmap = BitmapFactory.decodeFile(pictureFile.getAbsolutePath());

                    int[] pixels = new int[myBitmap.getHeight() * myBitmap.getWidth()];
                    myBitmap.getPixels(pixels, 0, myBitmap.getWidth(), 0, 0, myBitmap.getWidth(), myBitmap.getHeight());

                    System.out.println(Arrays.toString(pixels));
                    System.out.println(pixels.length);

                    // int[][] faceArrays = new int[identifiedFaces.length][];

                    int j = 0;
                    // Hard-create the subpixel arrays corresponding to the faces
                    for (Camera.Face face : identifiedFaces) {
                        int bottom = (int) Math.min((face.rect.bottom + 1000.0) / 2000.0, 1.0) * pictureHeight;
                        int left = (int) Math.max((face.rect.left + 1000.0) / 2000.0, 0.0) * pictureWidth;
                        int right = (int) Math.min((face.rect.right + 1000.0) / 2000.0, 1.0) * pictureWidth;
                        int top = (int) Math.max((face.rect.top + 1000.0) / 2000.0, 0.0) * pictureHeight;

                        int faWidth = (right - left + 1);
                        int faHeight = (bottom - top + 1);
                        int[] faceArray = new int[faWidth * faHeight];
                        int i = 0;
                        for (int r = top; r <= bottom; r++) {
                            for (int c = left; c <= right; c++) {
                                faceArray[i++] = pixels[r * pictureWidth + c];
                            }
                        }

                        String bestMatch = null;
                        int minDiff = Integer.MAX_VALUE;

                        // Scale (appropriately) our database photos down to the size of these subarrays
                        List<File> files = getListFiles(new File(getExternalFilesDir(DIRECTORY_RINGTONES).toString()));
                        System.out.println("num files: " + files.size());
                        for (File file : files) {
                            if (!file.getAbsolutePath().endsWith("jpg")) {
                                continue;
                            }
                            System.out.println("go: " + file.getAbsolutePath());
                            Bitmap dbBitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
                            Bitmap scaledBitmap = getResizedBitmap(dbBitmap, faWidth, faHeight);
                            int[] filePxs = new int[scaledBitmap.getHeight() * scaledBitmap.getWidth()];
                            scaledBitmap.getPixels(filePxs, 0, scaledBitmap.getWidth(), 0, 0, scaledBitmap.getWidth(), scaledBitmap.getHeight());

                            System.out.println("filepxs:" + filePxs.length);
                            System.out.println("facearray:" + faceArray.length);
                            // SSD-match versus all of the faces in the database
                            System.out.println("doing it");
                            int diff = ssd(filePxs, faceArray);
                            if (diff < minDiff) {
                                bestMatch = file.getAbsolutePath();
                                minDiff = diff;
                            }
                        }

                        // Return the name of the best possible match
                        System.out.println("Match!: " + bestMatch);
                    }

                }

                Log.d("picture", "wrote file");
            } catch (FileNotFoundException e) {
                Log.d(TAG, "File not found: " + e.getMessage());
            } catch (IOException e) {
                Log.d(TAG, "Error accessing file: " + e.getMessage());
            }
        }
    };

    /**
     * Sets the faces for the overlay view, so it can be updated
     * and the face overlays will be drawn again.
     */
    private Camera.FaceDetectionListener faceDetectionListener = new Camera.FaceDetectionListener() {
        @Override
        public void onFaceDetection(Camera.Face[] faces, Camera camera) {
            System.out.println("Identified a face");
            identifiedFaces = faces;

            // Create an instance of Camera
            try {
                mCamera.takePicture(null, null, mPicture);
                Log.d("takePicture", "call takePicture");
            } catch (Exception e) {
                Log.d("ERROR", "Failed to get camera: " + e.getMessage());
            }
        }
    };

//    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {
//
//        @Override
//        public void onPictureTaken(byte[] data, Camera camera) {
//            Log.d("onPictureTaken", "saved file");
//            File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
//            if (pictureFile == null){
//                Log.d(TAG, "Error creating media file, check storage permissions: ");
//                return;
//            }
//
//            try {
//                FileOutputStream fos = new FileOutputStream(pictureFile);
//                fos.write(data);
//                fos.close();
//
//                if(pictureFile.exists()){
//
//                    Bitmap myBitmap = BitmapFactory.decodeFile(pictureFile.getAbsolutePath());
//
//                    ImageView myImage = (ImageView) findViewById(R.id.image_preview);
//
//                    Bitmap myBitmap2 = BitmapFactory.decodeResource(getResources(), R.drawable.rolypoly);
//
//                    myImage.setImageBitmap(myBitmap);
//
//                    Matrix matrix = new Matrix();
//                    myImage.setScaleType(ImageView.ScaleType.MATRIX);   //required
//                    matrix.postRotate((float) 90, myImage.getDrawable().getBounds().width()/2,
//                            myImage.getDrawable().getBounds().height()/2);
//                    myImage.setImageMatrix(matrix);
//
//                    Log.d("ImagePreview", "inserted into ImageView");
//
//                }
//
//                Log.d("picture", "wrote file");
//            } catch (FileNotFoundException e) {
//                Log.d(TAG, "File not found: " + e.getMessage());
//            } catch (IOException e) {
//                Log.d(TAG, "Error accessing file: " + e.getMessage());
//            }
//            camera.startPreview();
//        }
//    };

    /** Create a File for saving an image or video */
    private File getOutputMediaFile(int type){

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE){
            mediaFile = new File(getFilesDir() + File.separator +
                    "IMG_"+ timeStamp + ".jpg");
        } else {
            return null;
        }

        Log.d("getOutPutMediaFile", mediaFile.toString());

        return mediaFile;
    }

    public Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // CREATE A MATRIX FOR THE MANIPULATION
        android.graphics.Matrix matrix = new android.graphics.Matrix();
        // RESIZE THE BIT MAP
        matrix.postScale(scaleWidth, scaleHeight);

        // "RECREATE" THE NEW BITMAP
        Bitmap resizedBitmap = Bitmap.createBitmap(
                bm, 0, 0, width, height, matrix, false);
        bm.recycle();
        return resizedBitmap;
    }

    private List<File> getListFiles(File parentDir) {
        List<File> inFiles = new ArrayList<>();
        Queue<File> files = new LinkedList<>();
        files.addAll(Arrays.asList(parentDir.listFiles()));
        while (!files.isEmpty()) {
            File file = files.remove();
            if (file.isDirectory()) {
                files.addAll(Arrays.asList(file.listFiles()));
            } else {
                inFiles.add(file);
            }
        }
        return inFiles;
    }

    public static int ssd(int[] A, int[] B)
    {
        int total = 0;
        for (int i = 0; i < A.length; i++) {
            int curr = A[i] - B[i];
            total += curr * curr;
        }

        return total;
    }

    public void toggleIdle(View view) {
        FrameLayout camera_view = (FrameLayout)findViewById(R.id.camera_preview);
        RelativeLayout idle_view = (RelativeLayout)findViewById(R.id.idle_layout);

        if (camera_view.getVisibility() == View.VISIBLE) {
            camera_view.setVisibility(View.INVISIBLE);
            idle_view.setVisibility(View.VISIBLE);
        } else {
            camera_view.setVisibility(View.VISIBLE);
            idle_view.setVisibility(View.INVISIBLE);
        }
    }
}
