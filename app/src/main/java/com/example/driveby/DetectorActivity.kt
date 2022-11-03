package com.example.driveby


import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.driveby.MyApplication.Companion.TAG
import kotlinx.coroutines.runBlocking
import org.opencv.android.CameraBridgeViewBase
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2
import org.opencv.core.*
import org.opencv.features2d.BOWImgDescriptorExtractor
import org.opencv.features2d.DescriptorMatcher
import org.opencv.features2d.FastFeatureDetector
import org.opencv.imgproc.Imgproc
import java.util.*


class DetectorActivity : AppCompatActivity(), CvCameraViewListener2 {
    private lateinit var mOpenCvCameraView: CameraBridgeViewBase





    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_detector)

        mOpenCvCameraView = findViewById(R.id.HelloOpenCvView)
        mOpenCvCameraView.setCameraPermissionGranted()
        mOpenCvCameraView.visibility = View.VISIBLE
        mOpenCvCameraView.setCvCameraViewListener(this)
        mOpenCvCameraView.enableView()



    }


    override fun onCameraViewStarted(width: Int, height: Int) {
    }

    override fun onCameraViewStopped() {

    }

    override fun onCameraFrame(inputFrame: CvCameraViewFrame): Mat? {
        var test:Mat?
            test= cirleSuchen(inputFrame)

        return test
    }


    fun cirleSuchen(inputFrame: CvCameraViewFrame): Mat?{
        val input = inputFrame.gray()
        val circles = Mat()
        Imgproc.blur(input, input, Size(7.0, 7.0), Point(0.0, 0.0))
        Imgproc.HoughCircles(
            input,
            circles,
            Imgproc.CV_HOUGH_GRADIENT,
            2.0,
            100.0,
            100.0,
            90.0,
            5,
            100
        )
        Log.i(TAG, "size: " + circles.cols() + ", " + circles.rows().toString())


        if (circles.cols() > 0) {
            for (x in 0 until Math.min(circles.cols(), 1)) {
                val circleVec = circles[0, x] ?: break
                val center = Point(
                    circleVec[0].toInt().toDouble(),
                    circleVec[1].toInt().toDouble()
                )
                val radius = circleVec[2].toInt()
                Imgproc.circle(input, center, 3, Scalar(255.0, 255.0, 255.0), 5)
                Imgproc.circle(input, center, radius, Scalar(255.0, 255.0, 255.0), 2)
            }
        }
        circles.release()
        input.release()
        return inputFrame.rgba()
    }

}
