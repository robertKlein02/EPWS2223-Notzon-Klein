package com.example.driveby

import android.graphics.Bitmap
import android.media.FaceDetector
import android.media.FaceDetector.Face
import android.os.Bundle
import android.view.Choreographer.FrameData
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.example.driveby.R
import org.opencv.android.CameraBridgeViewBase
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.objdetect.FaceDetectorYN

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
        mOpenCvCameraView.setMaxFrameSize(1000,1000)


    }


    override fun onCameraViewStarted(width: Int, height: Int) {


    }

    override fun onCameraViewStopped() {

    }

    override fun onCameraFrame(inputFrame: CameraBridgeViewBase.CvCameraViewFrame?): Mat {
        // Aufruf jeder einzelen Frames


        
        return inputFrame!!.rgba()
    }

    override fun onPause() {
        super.onPause()
        mOpenCvCameraView.disableView()
    }

    override fun onDestroy() {
        super.onDestroy()
        mOpenCvCameraView.disableView()
    }




}