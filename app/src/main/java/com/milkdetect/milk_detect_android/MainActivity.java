package com.milkdetect.milk_detect_android;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnTouchListener;
import android.view.SurfaceView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.mvc.imagepicker.ImagePicker;

import static org.opencv.android.Utils.matToBitmap;


public class MainActivity extends Activity {
    private static final String  TAG              = "OCVSample::Activity";
    static{ System.loadLibrary("opencv_java3"); }

    private ImageView mImage;
    private Mat mRgba;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());

        setContentView(R.layout.activity_main);
        mImage = (ImageView) findViewById(R.id.image);
        mImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Open Image Picker
                ImagePicker.pickImage(MainActivity.this, "Select your image:");
            }
        });
    }


    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && data != null && data.getData() != null){

            Bitmap bitmap = ImagePicker.getImageFromResult(this, requestCode, resultCode, data);
            String filename = "current.png";
            File sd = Environment.getExternalStorageDirectory();
            File dest = new File(sd, filename);

            FileOutputStream out = null;
            try {
                out = new FileOutputStream(dest);
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, out);
                out.flush();
                out.close();
                mImage.setImageBitmap(bitmap);

                mRgba = Imgcodecs.imread(dest.getPath());
                ProcessMilkStrip processMilkstrip = new ProcessMilkStrip();
                TextView textView = findViewById(R.id.result);

                Mat outMat = processMilkstrip.find_rectangle(mRgba);

                Bitmap bm = Bitmap.createBitmap(outMat.cols(), outMat.rows(),Bitmap.Config.ARGB_8888);
                matToBitmap(outMat, bm);
                mImage.setImageBitmap(bm);
                textView.setText(processMilkstrip.resultR + ", " + processMilkstrip.resultB + ", " + processMilkstrip.resultG);

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}