package xyz.owenjow.rolypoly;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;

/** A basic Camera preview class */
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    public static final String TAG = "YOUR-TAG-NAME";
    private SurfaceHolder mHolder;
    private Camera mCamera;

    // Holds the Face Detection result:
    private Camera.Face[] mFaces;

    // Draw rectangles and other fancy stuff:
    private FaceOverlayView mFaceView;

    /**
     * Sets the faces for the overlay view, so it can be updated
     * and the face overlays will be drawn again.
     */
    private Camera.FaceDetectionListener faceDetectionListener = new Camera.FaceDetectionListener() {
        @Override
        public void onFaceDetection(Camera.Face[] faces, Camera camera) {
            Log.d("onFaceDetection", "Number of Faces:" + faces.length);
            // Update the view now!
            mFaceView.setFaces(faces);
        }
    };

    public CameraPreview(Context context, Camera camera) {
        super(context);
        mCamera = camera;

        // Now create the OverlayView:
        mFaceView = new FaceOverlayView(this);
        addContentView(mFaceView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);
        // deprecated setting, but required on Android versions prior to 3.0
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public void surfaceCreated(SurfaceHolder holder) {
        // The Surface has been created, now tell the camera where to draw the preview.
        try {
            mCamera.setFaceDetectionListener(faceDetectionListener);
            mCamera.startFaceDetection();
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
        } catch (IOException e) {
            Log.d(TAG, "Error setting camera preview: " + e.getMessage());
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        // empty. Take care of releasing the Camera preview in your activity.
        mCamera.setPreviewCallback(null);
        mCamera.setFaceDetectionListener(null);
        mCamera.setErrorCallback(null);
        mCamera.release();
        mCamera = null;
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.

        if (mHolder.getSurface() == null){
            // preview surface does not exist
            return;
        }

        // stop preview before making changes
        try {
            mCamera.stopPreview();
        } catch (Exception e){
            // ignore: tried to stop a non-existent preview
        }

        // set preview size and make any resize, rotate or
        // reformatting changes here

        // start preview with new settings
        try {
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();

        } catch (Exception e){
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }

        Camera.Parameters parameters = mCamera.getParameters();
        if (this.getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) {
            parameters.set("orientation", "portrait");
            mCamera.setDisplayOrientation(90);
            parameters.setRotation(90);
            mCamera.startPreview();
        }
        else {
            // This is an undocumented although widely known feature
            parameters.set("orientation", "landscape");
            // For Android 2.2 and above
            mCamera.setDisplayOrientation(0);
            // Uncomment for Android 2.0 and above
            parameters.setRotation(0);
        }
    }
}