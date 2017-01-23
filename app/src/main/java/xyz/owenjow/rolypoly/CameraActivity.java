package xyz.owenjow.rolypoly;
import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.hardware.Camera;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static xyz.owenjow.rolypoly.CameraPreview.mFaceView;
import static xyz.owenjow.rolypoly.MainActivity.mCamera;

public class CameraActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_camera);

//        if (mFaceView != null && mFaceView.getParent() != null) {
//            ((ViewGroup) mFaceView.getParent()).removeView(mFaceView);
//        }
//
//        addContentView(mFaceView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
//                ViewGroup.LayoutParams.MATCH_PARENT));

        // Create an instance of Camera
        try{
            mCamera.takePicture(null, null, mPicture);
            Log.d("takePicture", "call takePicture");
        } catch (Exception e){
            Log.d("ERROR", "Failed to get camera: " + e.getMessage());
        }

    }

    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;

    /** Create a file Uri for saving an image or video */
    private Uri getOutputMediaFileUri(int type){
        return Uri.fromFile(getOutputMediaFile(type));
    }

    /** Create a File for saving an image or video */
    private File getOutputMediaFile(int type){

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE){
            mediaFile = new File(getFilesDir() + File.separator +
                    "IMG_"+ timeStamp + ".jpg");
        } else if(type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(getFilesDir() + File.separator +
                    "VID_"+ timeStamp + ".mp4");
        } else {
            return null;
        }

        Log.d("getOutPutMediaFile", mediaFile.toString());

        return mediaFile;
    }

    public static final String TAG = "YOUR-TAG-NAME";
    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
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

                    ImageView myImage = (ImageView) findViewById(R.id.image_preview);
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inMutable=true;
                    Bitmap myBitmap = BitmapFactory.decodeFile(
                            pictureFile.getAbsolutePath(),
                            options);

                    myImage.setImageBitmap(myBitmap);

                    Matrix matrix = new Matrix();

                    matrix.postRotate(90);

                    Bitmap scaledBitmap = Bitmap.createScaledBitmap(myBitmap,myImage.getDrawable().getBounds().width(),
                            myImage.getDrawable().getBounds().height(),true);

                    Bitmap rotatedBitmap = Bitmap.createBitmap(scaledBitmap , 0, 0, scaledBitmap .getWidth(), scaledBitmap .getHeight(), matrix, true);
                    myImage.setImageBitmap(rotatedBitmap);

                    Paint myRectPaint = new Paint();
                    myRectPaint.setStrokeWidth(5);
                    myRectPaint.setColor(Color.RED);
                    myRectPaint.setStyle(Paint.Style.STROKE);

                    Bitmap tempBitmap = Bitmap.createBitmap(rotatedBitmap.getWidth(), rotatedBitmap.getHeight(), Bitmap.Config.RGB_565);
                    Canvas tempCanvas = new Canvas(tempBitmap);
                    tempCanvas.drawBitmap(rotatedBitmap, 0, 0, null);

                    FaceDetector faceDetector = new
                            FaceDetector.Builder(getApplicationContext()).setTrackingEnabled(false)
                            .build();
                    if(!faceDetector.isOperational()){
                        Log.d("Face Detector", "could not set up face detector!");
                        return;
                    }

                    Frame frame = new Frame.Builder().setBitmap(rotatedBitmap).build();
                    SparseArray<Face> faces = faceDetector.detect(frame);

                    for(int i=0; i<faces.size(); i++) {
                        Face thisFace = faces.valueAt(i);
                        float x1 = thisFace.getPosition().x;
                        float y1 = thisFace.getPosition().y;
                        float x2 = x1 + thisFace.getWidth();
                        float y2 = y1 + thisFace.getHeight();
                        tempCanvas.drawRoundRect(new RectF(x1, y1, x2, y2), 2, 2, myRectPaint);
                    }
                    myImage.setImageDrawable(new BitmapDrawable(getResources(),tempBitmap));

                    Log.d("ImagePreview", "inserted into ImageView");

                }

                Log.d("picture", "wrote file");
            } catch (FileNotFoundException e) {
                Log.d(TAG, "File not found: " + e.getMessage());
            } catch (IOException e) {
                Log.d(TAG, "Error accessing file: " + e.getMessage());
            }
            camera.startPreview();
        }
    };

    public void releaseCamera(){
        if (mCamera != null){
            mCamera.release();        // release the camera for other applications
            mCamera = null;
            Log.d("picture", "release");
        }
    }

}


