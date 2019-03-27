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
    public double resultR;
    public double resultG;
    public double resultB;
    ProcessMilkStrip() {
    }

    public Mat find_rectangle(Mat input) {
        Mat coloredInputOriginal = input;
        Mat grayInputOriginal = new Mat();
        Imgproc.cvtColor(coloredInputOriginal,grayInputOriginal,Imgproc.COLOR_RGB2GRAY);

        Mat coloredInputCopy = input.clone();

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

        MatOfPoint rectContour = null;
        for (MatOfPoint c: contours) {
            double area = Imgproc.contourArea(c);
            if (area > 100000 && area < 180000) { // TODO: Can be replaced by percentages of screensize
                // Crop image points
                points = Imgproc.boundingRect(c);
                rectContour = c;
            }
        }

        if (points != null) {
            // Crop image to proceed
            coloredInput = coloredInput.submat(points);
            grayInput = grayInput.submat(points);

            Imgproc.drawContours(coloredInputCopy, Collections.singletonList(rectContour), -1, new Scalar(255, 255, 0));
        }

        List<Mat> bgr = new ArrayList<>();
        Core.split(coloredInput, bgr);

        Mat thresholded = new Mat(bgr.get(0).size(), CvType.CV_8U);
        Imgproc.threshold(bgr.get(0), thresholded, 170, 255, Imgproc.THRESH_BINARY);

        // Opening
        Imgproc.morphologyEx(thresholded, thresholded, Imgproc.MORPH_OPEN, kernel, new Point(-1, -1), 5);
        Imgproc.findContours(thresholded, contours, mat, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_NONE);
        // Mean values to be stored in a list
        double[][] means = new double[6][3];
        int count = 0;


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
                Point cNew;
                if (points != null) {
                    cNew = new Point((center.x + points.x), (center.y + points.y));
                } else {
                    cNew = new Point(center.x, center.y);
                }
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
            double cR = 0;
            double cG = 0;
            double cB = 0;

            Log.d("OUT", Arrays.toString(means));
            for (int i=0; i<6; ++i) {
                cR += means[i][0];
                cG += means[i][1];
                cB += means[i][2];
            }
            resultR = cR / 6.0;
            resultB = cG / 6.0;
            resultG = cB / 6.0;

            System.out.println("R:" + resultR + "-G:" + resultG + "-B:" + resultB);
        }

        return coloredInputCopy;
    }
}
