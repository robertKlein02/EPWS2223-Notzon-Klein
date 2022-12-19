package com.example.driveby.view


import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.driveby.MyApplication.Companion.TAG
import com.example.driveby.R
import com.example.driveby.Viewmodel
import com.example.driveby.receiver.Receiver
import com.example.driveby.sensor.SpeedSensor
import org.opencv.android.CameraBridgeViewBase
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2
import org.opencv.core.Mat
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc


class DetectorActivity : AppCompatActivity(), CvCameraViewListener2 {

    private lateinit var mOpenCvCameraView: CameraBridgeViewBase
    private lateinit var speedTextView:TextView
    private var viewmodel= Viewmodel()
    private val isConnected:MutableLiveData<Double> = viewmodel.speed
    private var speedSensorIstActive:Boolean=false


    override fun onBackPressed() {
        val a = Intent(Intent.ACTION_MAIN)
        a.addCategory(Intent.CATEGORY_HOME)
        a.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(a)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_detector)

        LocalBroadcastManager.getInstance(this).registerReceiver(Receiver(viewmodel), IntentFilter("testSpeed"))

        mOpenCvCameraView = findViewById(R.id.HelloOpenCvView)
        mOpenCvCameraView.setCameraPermissionGranted()
        mOpenCvCameraView.visibility = View.VISIBLE
        mOpenCvCameraView.setCvCameraViewListener(this)
        mOpenCvCameraView.enableView()


        speedTextView=findViewById(R.id.speed)
        speedTextView.setText(viewmodel.speed.value.toString())

        var i =Intent(this, SpeedSensor::class.java)
        if (!speedSensorIstActive){
            startService(i)
            speedSensorIstActive=true
        }


        isConnected.observe(this, Observer {
            newSpeed ->
            isConnected.postValue(newSpeed)
            speedTextView.text="$newSpeed  km/h"

        })





    }

    override fun onDestroy() {
        super.onDestroy()
        var i =Intent(this, SpeedSensor::class.java)
        if (speedSensorIstActive){
            stopService(i)
            speedSensorIstActive=false
        }

    }





    override fun onCameraViewStarted(width: Int, height: Int) {
    }

    override fun onCameraViewStopped() {

    }

    override fun onCameraFrame(inputFrame: CvCameraViewFrame): Mat? {
        var test:Mat?

            test= cirleSuchenUndUmkreisen(inputFrame)



        return test
    }


    fun cirleSuchenUndUmkreisen(inputFrame: CvCameraViewFrame): Mat?{
        val inputGrey = inputFrame.gray()
        val inputRGB = inputFrame.rgba()
        val circles = Mat()
        Imgproc.blur(inputGrey, inputGrey, Size(5.0, 5.0), Point(3.0, 3.0))
        Imgproc.HoughCircles(
            inputGrey,
            circles,
            Imgproc.CV_HOUGH_GRADIENT,
            2.0,
            1000.0,
            175.0,
            120.0,
            25,
            125
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
              //  Imgproc.circle(input, center, 3, Scalar(255.0, 255.0, 255.0), -1)

                Imgproc.circle(inputRGB, center, radius, Scalar(0.0, 255.0, 0.0), 2)
            }
        }
        circles.release()
        inputGrey.release()
        return inputRGB
    }

}
