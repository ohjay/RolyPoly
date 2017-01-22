package xyz.owenjow.rolypoly;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.hardware.Camera;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.widget.Button;
import android.widget.FrameLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class MainActivity extends AppCompatActivity {

    public static Camera mCamera = null;
    private CameraPreview mCameraPreview = null;

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

        try{
            releaseCamera();
            mCamera = Camera.open();//you can use open(int) to use different cameras
        } catch (Exception e){
            Log.d("ERROR", "Failed to get camera: " + e.getMessage());
        }

        if(mCamera != null) {
            mCameraPreview = new CameraPreview(this, mCamera);//create a SurfaceView to show camera data
            FrameLayout camera_view = (FrameLayout)findViewById(R.id.camera_preview);
            camera_view.addView(mCameraPreview);//add the SurfaceView to the layout

            Button captureButton = new Button(this);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER_VERTICAL | Gravity.RIGHT
            );
            captureButton.setLayoutParams(params);
            camera_view.addView(captureButton);

            Drawable cameraButton = getResources().getDrawable(R.drawable.round_button);
            cameraButton.setAlpha(170);
            captureButton.setBackground(cameraButton);

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

    public void releaseCamera(){
        if (mCamera != null){
            mCamera.release();        // release the camera for other applications
            mCamera = null;
        }
    }

    public void goIdentify(View view)
    {
        Intent intent = new Intent(this, IdentifyActivity.class);
        startActivity(intent);
    }

}
