package com.milkdetect.milk_detect_android;

import android.util.Log;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.opencv.imgcodecs.*; // imread, imwrite, etc

public class ProcessMilkStrip {
    ProcessMilkStrip() {
    }

    public Mat find_rectangle(CameraBridgeViewBase.CvCameraViewFrame inputFile) {
        Mat coloredInputOriginal = inputFile.rgba();
        Mat grayInputOriginal = inputFile.gray();

        // Copy to temp mats
        Mat coloredInput = new Mat(coloredInputOriginal.size(), CvType.CV_8SC3);
        coloredInputOriginal.copyTo(coloredInput);

        Mat grayInput = new Mat(coloredInputOriginal.size(), CvType.CV_8SC1);
        grayInputOriginal.copyTo(grayInput);
        //
        Imgproc.threshold(grayInput, grayInput, 80, 255, Imgproc.THRESH_BINARY);

        final Size kernelSize = new Size(3, 3);
        final Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, kernelSize);
        Imgproc.morphologyEx(grayInput, grayInput, Imgproc.MORPH_OPEN, kernel, new Point(-1,-1), 5);

        // Find contours of rectangle
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Mat mat = new Mat(grayInput.size(), CvType.CV_8U);
        Imgproc.findContours(grayInput, contours, mat, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_NONE);

        // Find the outer most rectangle
        Rect points = null;
        Mat cImg = new Mat(coloredInput.size(), CvType.CV_8UC3);
        coloredInput.copyTo(cImg);

        for (MatOfPoint c: contours) {
            double area = Imgproc.contourArea(c);
            if (area > 10000) { // TODO: Can be replaced by percentages of screensize
                // Crop image points
                points = Imgproc.boundingRect(c);
                Imgproc.drawContours(cImg, Collections.singletonList(c), -1, new Scalar(255, 255, 0));
            }
        }

        if (points != null) {
            // Crop image to proceed
            coloredInput = coloredInput.submat(points);
            grayInput = grayInput.submat(points);
        }
        List<Mat> bgr = new ArrayList<>();
        Core.split(coloredInput, bgr);

        Mat thresholded = new Mat(bgr.get(0).size(), CvType.CV_8U);
        Imgproc.threshold(bgr.get(0), thresholded, 170, 255, Imgproc.THRESH_BINARY);

        // Opening
        Imgproc.morphologyEx(thresholded, thresholded, Imgproc.MORPH_OPEN, kernel, new Point(-1, -1), 5);
        Imgproc.findContours(thresholded, contours, mat, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_NONE);
        // Mean values to be stored in a list
        double[][] means = new double[6][4];
        int count = 0;

        Mat coloredInputCopy = inputFile.rgba();

        for (MatOfPoint c: contours) {
            Point center = new Point();
            float[] radius = new float[1];
            Imgproc.minEnclosingCircle(new MatOfPoint2f(c.toArray()), center, radius);
            double area = Imgproc.contourArea(c);
            Log.d("CV", "Area" +  area);
            Log.d("CV", "Radius" + radius[0]);
            Log.d("CV", "Center" + center);


            if (radius[0] > 20 && radius[0] < 40
                    && Math.abs(area - 3.14d * radius[0] * radius[0]) < 1200) { // TODO: Can be replaced by percentages of screensize
                Point cNew = new Point((center.x + points.x), (center.y + points.y));
                Imgproc.drawContours(cImg, Collections.singletonList(c), -1, new Scalar(0, 255, 0));
                Mat mask = new Mat(grayInput.size(), 0);
                Imgproc.fillPoly(mask, Collections.singletonList(c), new Scalar(255));
                Imgproc.circle(coloredInputCopy, cNew, (int) radius[0], new Scalar(255, 0, 0));

                Scalar mean_val = Core.mean(coloredInput, mask);
                if (count >= 6) {
                    break;
                }

                means[count] = mean_val.val;
                count += 1;
            }
        }
        if (count != 6) {
            // Wrong count
        } else {
            Log.d("OUT", Arrays.toString(means));
        }

        return coloredInputCopy;
    }
}
