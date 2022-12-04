package com.example.driveby.view


import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
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
import org.opencv.core.*
import org.opencv.imgproc.Imgproc


class DetectorActivity : AppCompatActivity(), CvCameraViewListener2 {
    private lateinit var mOpenCvCameraView: CameraBridgeViewBase
    private lateinit var speedTextView:TextView
    private var viewmodel= Viewmodel()
    private val isConnected:MutableLiveData<Double> = viewmodel.speed






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
        startService(i)

        isConnected.observe(this, Observer {
            newSpeed ->
            isConnected.postValue(newSpeed)
            speedTextView.text=newSpeed.toString()

        })





    }

    override fun onDestroy() {
        var i =Intent(this, SpeedSensor::class.java)
        stopService(i)
        super.onDestroy()

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
        val input = inputFrame.gray()
        val circles = Mat()
        Imgproc.blur(input, input, Size(1.0, 1.0), Point(0.0, 0.0))
        Imgproc.HoughCircles(
            input,
            circles,
            Imgproc.CV_HOUGH_GRADIENT,
            1.0,
            60.0,
            200.0,
            20.0,
            30,
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
              //  Imgproc.circle(input, center, 3, Scalar(255.0, 255.0, 255.0), -1)
                Imgproc.circle(input, center, radius, Scalar(255.0, 255.0, 255.0), 2)
            }
        }
        circles.release()
        input.release()
        return inputFrame.rgba()
    }

}
