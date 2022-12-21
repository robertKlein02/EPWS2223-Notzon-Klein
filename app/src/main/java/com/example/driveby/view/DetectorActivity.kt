package com.example.driveby.view


import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
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
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.ObjectDetector
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Semaphore
import org.opencv.android.CameraBridgeViewBase
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2
import org.opencv.dnn.TextDetectionModel
import org.opencv.dnn.TextRecognitionModel
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import kotlin.math.abs


class DetectorActivity : AppCompatActivity(), CvCameraViewListener2 {

    private var rows = 0
    private var cols = 0
    private var left = 0
    private var width = 0
    private var top = 0.0
    private var bm: Bitmap? = null
    private lateinit var mOpenCvCameraView: CameraBridgeViewBase
    private lateinit var speedTextView:TextView
    private var viewmodel= Viewmodel()
    private val isConnected:MutableLiveData<Double> = viewmodel.speed
    private var speedSensorIstActive:Boolean=false
    private lateinit var zeichenBereich: Rect

    private lateinit var textOpen: TextDetectionModel
    private lateinit var textLeser: TextRecognizer
    private lateinit var objRecognizer: ObjectDetector


    private var analyzeIsBusy = false

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



        textLeser = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        objRecognizer= ObjectDetection.getClient(ObjectDetectorOptions.DEFAULT_OPTIONS)


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



    override fun onCameraViewStarted(w: Int, h: Int) {
        rows = h
        cols = w
        left = rows / 8
        width = cols - left
        top = rows / 2.5

    }

    override fun onCameraViewStopped() {

    }

    override fun onCameraFrame(inputFrame: CvCameraViewFrame): Mat? {
        var mat: Mat? =null


        mat= cirleSuchenUndUmkreisen(inputFrame)

        return mat
    }




    fun cirleSuchenUndUmkreisen(inputFrame: CvCameraViewFrame): Mat {

        val inputGrey = inputFrame.gray()
        val inputRGB = inputFrame.rgba()
        val circles = Mat()

        Imgproc.blur(inputGrey, inputGrey, Size(5.0, 5.0), Point(3.0, 3.0))
        Imgproc.GaussianBlur(inputRGB, inputRGB, Size(5.0, 5.0), 3.0)

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

                val rectSideVal = radius * 2 + 20

                zeichenBereich = Rect(
                    (center.x - radius - 10).toInt(),
                    (center.y - radius - 10).toInt(), rectSideVal, rectSideVal
                )
                Imgproc.circle(inputRGB, center, radius, Scalar(0.0, 255.0, 0.0), 2)

                cricleRead(inputRGB,zeichenBereich,radius)

                circles.release()
            }
        }

        circles.release()
        return inputRGB
    }


    private var signSpeed = ""


    private fun cricleRead(img: Mat?, roi: Rect?, radius: Int) {

        val t = Thread {
            analyzeIsBusy=true
            val copy: Mat
            try {
                copy = Mat(img, roi)

                // bimap mit der size des schildes erstelleb

                bm = Bitmap.createBitmap(
                    abs(radius * 2 + 20),
                    abs(radius * 2 + 20),
                    Bitmap.Config.ARGB_8888)

                Utils.matToBitmap(copy, bm)

            } catch (e: Exception) {
                bm = null
            }
            if (bm != null) {
                val image = InputImage.fromBitmap(bm!!, 0)


                textLeser.process(image)
                    .addOnSuccessListener { visionText ->
                        for (block in visionText.textBlocks) {
                            if (signSpeed != block.text) {
                                signSpeed = block.text
                                if (signSpeed=="10") findViewById<ImageView>(R.id.imageView).setImageResource(R.drawable.limit10)
                                if (signSpeed=="20") println("20 KM/H")
                                if (signSpeed=="30") findViewById<ImageView>(R.id.imageView).setImageResource(R.drawable.limit30)
                                if (signSpeed=="40") println("40 KM/H")
                                if (signSpeed=="50") println("50 KM/H")
                                if (signSpeed=="60") println("60 KM/H")
                                if (signSpeed=="70") println("70 KM/H")
                                if (signSpeed=="80") println("80 KM/H")
                                if (signSpeed=="90") println("90 KM/H")
                                if (signSpeed=="100") println("100 KM/H")

                            }
                        }
                    }
            }
        }


        val textThread = Thread(t)
        textThread.start()
        analyzeIsBusy=false
    }
}


