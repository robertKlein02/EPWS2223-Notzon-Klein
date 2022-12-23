package com.example.driveby.view


import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
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
import org.opencv.android.CameraBridgeViewBase
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2
import org.opencv.dnn.TextDetectionModel
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.label.ImageLabeler
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import kotlin.math.abs


class DetectorActivity : AppCompatActivity(), CvCameraViewListener2 {
    private var imgWidth = 0
    private var imgHeight = 0
    private var rows = 0
    private var cols = 0
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
    private lateinit var labeler: ImageLabeler





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
        labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)

        mOpenCvCameraView = findViewById(R.id.HelloOpenCvView)
        mOpenCvCameraView.setCameraPermissionGranted()
        mOpenCvCameraView.visibility = View.VISIBLE
        mOpenCvCameraView.setCvCameraViewListener(this)
        mOpenCvCameraView.enableView()
        mOpenCvCameraView.enableFpsMeter()


        imgWidth= 720
        imgHeight= 540


        mOpenCvCameraView.setMaxFrameSize(imgWidth,imgHeight)



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
            for (x in 0 until Math.min(circles.cols(), 5)) {
                val circleVec = circles[0, x] ?: break
                val center = Point(
                    circleVec[0],
                    circleVec[1]
                )


                val radius = circleVec[2].toInt()

                val rectSideVal = radius * 2 + 20

                zeichenBereich = Rect(
                    (center.x - radius - 10).toInt(),
                    (center.y - radius - 10).toInt(), rectSideVal, rectSideVal
                )
                Imgproc.circle(inputRGB, center, radius, Scalar(0.0, 255.0, 0.0), 2)

                // Problem ! Wenn der Kreis über den Rand geht ist die Matrix nicht ganz gefüllt und die App stürzt ab!
                if (circleVec[0]-radius >= 20
                    && circleVec[0]+radius <= imgWidth-20
                    &&  circleVec[1]-radius >= 20
                    &&circleVec[1]+radius <= imgHeight-20){

                    Log.i("xxx","test")
                    cricleRead(inputRGB,zeichenBereich,radius)
                }


                circles.release()
            }
        }
        circles.release()
        return inputRGB
    }




    private var signSpeedMybe = "0"
    private var signSpeedNow = "0"

    // Sicherheits Funktion
    fun speedSet(int: Int){

        var image = findViewById<ImageView>(R.id.imageView)
        var speedtoInt = signSpeedNow.toInt()

        if (int==10) {
            if (signSpeedNow.toInt()==0) {
                image.setImageResource(R.drawable.limit10)
                signSpeedNow="$int"
            }
            if (abs( speedtoInt-int)<50){
                signSpeedNow="$int"
                image.setImageResource(R.drawable.limit10)
            }
        }

        if (int==20) {
            if (signSpeedNow.toInt()==0){
                image.setImageResource(R.drawable.limit20)
                signSpeedNow="$int"
            }
            if (abs( speedtoInt-int)<50){
                signSpeedNow="$int"
                image.setImageResource(R.drawable.limit20)
            }
        }

        if (int==30) {
            if (signSpeedNow.toInt()==0){
                image.setImageResource(R.drawable.limit30)
                signSpeedNow="$int"
            }
            if (abs( speedtoInt-int)<50){
                signSpeedNow="$int"
                image.setImageResource(R.drawable.limit30)
            }
        }

        if (int==40) {
            if (signSpeedNow.toInt()==0){
                image.setImageResource(R.drawable.limit40)
                signSpeedNow="$int"
            }
            if (abs( speedtoInt-int)<50){
                signSpeedNow="$int"
                image.setImageResource(R.drawable.limit40)
            }
        }

        if (int==50) {
            if (signSpeedNow.toInt()==0){
                image.setImageResource(R.drawable.limit50)
                signSpeedNow="$int"
            }
            if (abs( speedtoInt-int)<50){
                signSpeedNow="$int"
                image.setImageResource(R.drawable.limit50)
            }
        }

        if (int==60) {
            if (signSpeedNow.toInt()==0){
                image.setImageResource(R.drawable.limit60)
                signSpeedNow="$int"
            }
            if (abs( speedtoInt-int)<50){
                signSpeedNow="$int"
                image.setImageResource(R.drawable.limit60)
            }
        }

        if (int==70) {
            if (signSpeedNow.toInt()==0){
                image.setImageResource(R.drawable.limit70)
                signSpeedNow="$int"
            }
            if (abs( speedtoInt-int)<50){
                signSpeedNow="$int"
                image.setImageResource(R.drawable.limit70)
            }
        }

        if (int==80) {
            if (signSpeedNow.toInt()==0){
                image.setImageResource(R.drawable.limit80)
                signSpeedNow="$int"
            }
            if (abs( speedtoInt-int)<50){
                signSpeedNow="$int"
                image.setImageResource(R.drawable.limit80)
            }
        }

        if (int==90) {
            if (signSpeedNow.toInt()==0){
                image.setImageResource(R.drawable.limit90)
                signSpeedNow="$int"
            }
            if (abs( speedtoInt-int)<50){
                signSpeedNow="$int"
                image.setImageResource(R.drawable.limit90)
            }
        }

        if (int==100) {
            if (signSpeedNow.toInt()==0){
                image.setImageResource(R.drawable.limit100)
                signSpeedNow="$int"
            }
            if (abs( speedtoInt-int)<50){
                signSpeedNow="$int"
                image.setImageResource(R.drawable.limit100)
            }
        }

        if (int==110) {
            if (signSpeedNow.toInt()==0){
                image.setImageResource(R.drawable.limit110)
                signSpeedNow="$int"
            }
            if (abs( speedtoInt-int)<50){
                signSpeedNow="$int"
                image.setImageResource(R.drawable.limit110)
            }
        }

        if (int==120) {
            if (signSpeedNow.toInt()==0){
                image.setImageResource(R.drawable.limit120)
                signSpeedNow="$int"
            }
            if (abs( speedtoInt-int)<50){
                signSpeedNow="$int"
                image.setImageResource(R.drawable.limit120)
            }
        }

    }

    private fun cricleRead(img: Mat?, roi: Rect?, radius: Int) {
        val t = Runnable {
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


             //   // zeigt alle label vom Kreisinhalt
             //   labeler.process(image)
             //       .addOnSuccessListener { objRec ->
             //           for (i in objRec){
             //               println(i.text)
             //           }
             //       }


                textLeser.process(image).addOnSuccessListener { visionText ->

                        for (block in visionText.textBlocks) {
                            if (signSpeedMybe != block.text) {
                                signSpeedMybe = block.text
                                if (signSpeedMybe=="10") speedSet(10)
                                if (signSpeedMybe=="20") speedSet(20)
                                if (signSpeedMybe=="30") speedSet(30)
                                if (signSpeedMybe=="40") speedSet(40)
                                if (signSpeedMybe=="50") speedSet(50)
                                if (signSpeedMybe=="60") speedSet(60)
                                if (signSpeedMybe=="70") speedSet(70)
                                if (signSpeedMybe=="80") speedSet(80)
                                if (signSpeedMybe=="90") speedSet(90)
                                if (signSpeedMybe=="100") speedSet(100)
                                if (signSpeedMybe=="110") speedSet(110)
                                if (signSpeedMybe=="120") speedSet(120)
                            }
                        }
                    }
            }
        }

        t.run()
        analyzeIsBusy=false
    }


}


