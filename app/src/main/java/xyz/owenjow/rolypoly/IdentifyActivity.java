package xyz.owenjow.rolypoly;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

public class IdentifyActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);





        setContentView(R.layout.activity_identify);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
    }

    //Source: http://docs.opencv.org/2.4/doc/tutorials/introduction/desktop_java/java_dev_intro.html#running-sbt-samples
    // Detects faces in an image, draws boxes around them, and writes the results
    // to "faceDetection.png".
    //
    class DetectFaceDemo {
        public void run() {
            System.out.println("\nRunning DetectFaceDemo");

            // Create a face detector from the cascade file in the resources
            // directory.
            CascadeClassifier faceDetector = new CascadeClassifier(getClass().getResource("/lbpcascade_frontalface.xml").getPath().substring(1).replace("%20", " "));
            Mat image = Imgcodecs.imread(getClass().getResource("/lena.png").getPath().substring(1).replace("%20", " "));

            // Detect faces in the image.
            // MatOfRect is a special container class for Rect.
            MatOfRect faceDetections = new MatOfRect();
            faceDetector.detectMultiScale(image, faceDetections);

            System.out.println(String.format("Detected %s faces", faceDetections.toArray().length));

            // Draw a bounding box around each face.
            for (Rect rect : faceDetections.toArray()) {
                Imgproc.rectangle(image, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(0, 255, 0));
            }

            // Save the visualized detection.
            String filename = "faceDetection.png";
            System.out.println(String.format("Writing %s", filename));
            Imgcodecs.imwrite(filename, image);
        }
    }
}
