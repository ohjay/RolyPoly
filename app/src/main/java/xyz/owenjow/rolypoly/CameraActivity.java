package xyz.owenjow.rolypoly;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.hardware.Camera;
import android.util.Log;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static android.content.res.Configuration.ORIENTATION_LANDSCAPE;
import static xyz.owenjow.rolypoly.CameraPreview.mFaceView;
import static xyz.owenjow.rolypoly.MainActivity.mCamera;

public class CameraActivity extends Activity {

    int mOrientation;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_camera);

        mOrientation = getResources().getConfiguration().orientation;

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

                    int rotation = getCameraPhotoOrientation(
                            CameraActivity.this,
                            Uri.fromFile(pictureFile),
                            pictureFile.getAbsolutePath());

                    Log.d("rotation number", rotation + "");

                    Bitmap rotatedBitmap = rotateImage(myBitmap, rotation);
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
                    final SparseArray<Face> faces = faceDetector.detect(frame);

                    myImage.setOnTouchListener(new View.OnTouchListener()
                    {
                        @Override
                        public boolean onTouch(View v, MotionEvent event)
                        {
                            float touchX = event.getX();
                            float touchY = event.getY();
                            setOrientationCoordinates(touchX, touchY);

                            System.out.println("touchX: " + String.valueOf(touchX));
                            System.out.println("touchY: " + String.valueOf(touchY));
                            switch(event.getAction()){
                                case MotionEvent.ACTION_DOWN:
                                    System.out.println("Touching down!");
                                    for(int i=0; i<faces.size(); i++){
                                        Face face = faces.valueAt(i);
                                        RectF faceRect = rectFromFace(face);
                                        System.out.println("rect " + String.valueOf(faceRect.left) + " " + String.valueOf(faceRect.right)
                                                + " " + String.valueOf(faceRect.top) + " " + String.valueOf(faceRect.bottom));
                                        if(faceRect.contains(touchX, touchY)){
                                            System.out.println("Touched Face Rectangle.");

                                            RelativeLayout layout = (RelativeLayout) v.findViewById(R.id.activity_camera);
                                            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                                                    50,
                                                    30);

                                            EditText faceEdit= new EditText(CameraActivity.this);
                                            faceEdit.setId(i);
                                            faceEdit.setLayoutParams(params);
                                            faceEdit.setHint("Enter student name");

                                            layout.addView(faceEdit);

                                        }
                                    }
                                    break;
                                case MotionEvent.ACTION_UP:
                                    System.out.println("Touching up!");
                                    break;
                                case MotionEvent.ACTION_MOVE:
                                    System.out.println("Sliding your finger around on the screen.");
                                    break;
                            }
                            return true;
                        }
                    });

                    for(int i=0; i<faces.size(); i++) {
                        Face thisFace = faces.valueAt(i);
                        tempCanvas.drawRoundRect(rectFromFace(thisFace), 2, 2, myRectPaint);
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

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mOrientation = newConfig.orientation;
    }

    public void setOrientationCoordinates(float x, float y) {
        if (mOrientation == ORIENTATION_LANDSCAPE) {
            float temp = x;
            x = y;
            y = temp;
            System.out.println("Changed coordinates");
        }
    }

    public static RectF rectFromFace(Face face) {
        float x1 = face.getPosition().x;
        float y1 = face.getPosition().y;
        float x2 = x1 + face.getWidth();
        float y2 = y1 + face.getHeight();
        Log.d("rectFromFace", "new rectangle");
        return new RectF(x1, y1, x2, y2);
    }

    public static Bitmap rotateImage(Bitmap source, int angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(),
                matrix, true);
    }

    public int getCameraPhotoOrientation(Context context, Uri imageUri, String imagePath){
        int rotate = 0;
        try {
            context.getContentResolver().notifyChange(imageUri, null);
            File imageFile = new File(imagePath);

            ExifInterface exif = new ExifInterface(imageFile.getAbsolutePath());
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotate = 270;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotate = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotate = 90;
                    break;
            }

            Log.i("RotateImage", "Exif orientation: " + orientation);
            Log.i("RotateImage", "Rotate value: " + rotate);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return rotate;
    }

    public void releaseCamera(){
        if (mCamera != null){
            mCamera.release();        // release the camera for other applications
            mCamera = null;
            Log.d("picture", "release");
        }
    }

}


