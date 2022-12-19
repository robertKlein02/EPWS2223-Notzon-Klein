package com.example.driveby.view


import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
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
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.ObjectDetector
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import org.opencv.android.CameraBridgeViewBase
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import kotlin.math.abs


class DetectorActivity : AppCompatActivity(), CvCameraViewListener2 {

    private var bm: Bitmap? = null
    private lateinit var mOpenCvCameraView: CameraBridgeViewBase
    private lateinit var speedTextView:TextView
    private var viewmodel= Viewmodel()
    private val isConnected:MutableLiveData<Double> = viewmodel.speed
    private var speedSensorIstActive:Boolean=false
    private lateinit var zeichenBereuch: Rect
    private lateinit var textRecognizer: TextRecognizer
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

        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
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





    override fun onCameraViewStarted(width: Int, height: Int) {
    }

    override fun onCameraViewStopped() {

    }

    override fun onCameraFrame(inputFrame: CvCameraViewFrame): Mat? {
        

        if (analyzeIsBusy)return inputFrame.rgba()
        else return cirleSuchenUndUmkreisen(inputFrame)

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

                val rectSideVal = radius * 2 + 20

                zeichenBereuch = Rect(
                    (center.x - radius - 10).toInt(),
                    (center.y - radius - 10).toInt(), rectSideVal, rectSideVal
                )
                Imgproc.circle(inputRGB, center, radius, Scalar(0.0, 255.0, 0.0), 2)
                if (!analyzeIsBusy) kreisLesen(inputRGB,zeichenBereuch,radius)
                circles.release()
            }
        }
        circles.release()
        return inputRGB
    }


    private var signSpeed = ""


    private fun kreisLesen(img: Mat?, roi: Rect?, radius: Int) {
        val runnable = Runnable {
            analyzeIsBusy = true
            val copy: Mat
            try {
                copy = Mat(img, roi)
                // bimap mit der size des schildes erstelleb
                bm = Bitmap.createBitmap(
                    abs(radius * 2 + 20),
                    abs(radius * 2 + 20),
                    Bitmap.Config.ARGB_8888
                )
                Utils.matToBitmap(copy, bm)
            } catch (e: Exception) {
                bm = null
            }
            if (bm != null) {
                val image = InputImage.fromBitmap(bm!!, 0)
                textRecognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        for (block in visionText.textBlocks) {
                            if (signSpeed != block.text) {
                                signSpeed = block.text

                                if (signSpeed=="30") println("ddhjfgsdkasdfbhsadhfgskafdghxdsiawöofhlsioöadghkuiljsaoösfgkuhigkzdhilsafgkuzihlsdafgukzhileafgsdkzxdilhaefgkszd")


                            }
                        }
                    }
            }
            analyzeIsBusy = false
        }
        if (!analyzeIsBusy) {
            val textDetectionThread = Thread(runnable)
            textDetectionThread.run()
        }
    }

}
