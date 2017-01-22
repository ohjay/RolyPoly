package xyz.owenjow.rolypoly;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.hardware.Camera;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.ImageView;

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
        setContentView(R.layout.activity_main);

        addContentView(mFaceView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // Create an instance of Camera
        try{
            mCamera.takePicture(null, null, mPicture);
            Log.d("picture", "call takePicture");
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

        Log.d("picture", mediaFile.toString());

        return mediaFile;
    }

    public static final String TAG = "YOUR-TAG-NAME";
    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            Log.d("picture", "took picture in on picture taken");
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

                    ImageView myImage = (ImageView) findViewById(R.id.image_preview);

                    myImage.setImageBitmap(myBitmap);

                }

                Log.d("picture", "wrote file");
            } catch (FileNotFoundException e) {
                Log.d(TAG, "File not found: " + e.getMessage());
            } catch (IOException e) {
                Log.d(TAG, "Error accessing file: " + e.getMessage());
            }
            camera.startPreview();
            releaseCamera();
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


