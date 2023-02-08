package com.example.driveby.view


import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.driveby.MyApplication.Companion.TAG
import com.example.driveby.R
import com.example.driveby.ViewmodelSpeedLimit
import com.example.driveby.sensor.SpeedSensor
import com.google.firebase.FirebaseApp
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeler
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.objects.ObjectDetector
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import org.opencv.android.CameraBridgeViewBase
import org.opencv.android.CameraBridgeViewBase.*
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.dnn.TextDetectionModel
import org.opencv.imgproc.Imgproc
import java.util.*
import kotlin.math.abs


class DetectorActivity : AppCompatActivity(), CvCameraViewListener2,
    TextToSpeech.OnInitListener {
    
    private var imgWidth = 0
    private var imgHeight = 0
    private var rows = 0
    private var cols = 0
    private var bm: Bitmap? = null
    private var speed:Double=0.0

    private var soundIstActive:Boolean = false

    private lateinit var ttsSpeed: TextToSpeech

    private lateinit var mOpenCvCameraView: CameraBridgeViewBase
    private lateinit var speedTextView:TextView

    private var viewmodel= ViewmodelSpeedLimit()
    private val isConnected:MutableLiveData<Double> = viewmodel.speed
    private var speedSensorIstActive:Boolean=false
    private lateinit var zeichenBereich: Rect


    private lateinit var textOpen: TextDetectionModel
    private lateinit var textLeser: TextRecognizer
    private lateinit var objRecognizer: ObjectDetector
    private lateinit var labeler: ImageLabeler
    private lateinit var sound :ImageButton

    val database = Firebase.database
    val myRef = database.getReference("message")



    private var analyzeIsBusy = false

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val a = Intent(Intent.ACTION_MAIN)
        a.addCategory(Intent.CATEGORY_HOME)
        a.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(a)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        myRef.setValue("Hello, World!")

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_detector)



        soundIstActive=true

        LocalBroadcastManager.getInstance(this)
            .registerReceiver(viewmodel.Receiver(), IntentFilter("testSpeed"))

        textLeser = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)


        ttsSpeed = TextToSpeech(this, this)
        sound =findViewById(R.id.imageButton)
        mOpenCvCameraView = findViewById(R.id.HelloOpenCvView)
        mOpenCvCameraView.setCameraPermissionGranted()
        mOpenCvCameraView.visibility = View.VISIBLE
        mOpenCvCameraView.setCvCameraViewListener(this)
        mOpenCvCameraView.enableView()
        mOpenCvCameraView.enableFpsMeter()
        sizeSetterFrame()


        mOpenCvCameraView.setMaxFrameSize(imgWidth, imgHeight)

        //für test
        // speedSet(50)

        speedTextView = findViewById(R.id.speed)
        speedTextView.setText(viewmodel.speed.value.toString())

        val i = Intent(this, SpeedSensor::class.java)
        if (!speedSensorIstActive) {
            startService(i)
            speedSensorIstActive = true
        }

        Runnable {
            isConnected.observe(this, Observer { newSpeed ->
                isConnected.postValue(newSpeed)
                speedTextView.text = "$newSpeed  km/h"
                speed = newSpeed
                speedTextColo(signSpeedNow.toInt(), speed)
            })
        }.run()


        sound.setOnClickListener {

            if (soundIstActive){
                soundIstActive=false
                Log.i("active","true")
                sound.setBackgroundResource(R.drawable.volume_off_white_24dp)
            }else{
                soundIstActive=true
                Log.i("active","false")
                sound.setBackgroundResource(R.drawable.volume_on_white_24dp)
            }

        }
    }


    override fun onDestroy() {
        super.onDestroy()
        val i =Intent(this, SpeedSensor::class.java)
        
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




   private fun cirleSuchenUndUmkreisen(inputFrame: CvCameraViewFrame): Mat {

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
                    circleVec[0],
                    circleVec[1]
                )

                val radius = circleVec[2].toInt()
                val rectSideVal = radius * 2 + 20

                zeichenBereich = Rect(
                    (center.x - radius - 10).toInt(),
                    (center.y - radius - 10).toInt(), rectSideVal, rectSideVal
                )

                Imgproc.circle(inputRGB, center, radius, Scalar(0.0, 255.0, 0.0), 3)

                // Problem !!!!
                // Wenn Circle über den Rand geht -> crash
                if (circleVec[0]-radius >= 10
                    && circleVec[0]+radius <= imgWidth-10
                    &&  circleVec[1]-radius >= 10
                    &&circleVec[1]+radius <= imgHeight-10){

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
   private fun speedSet(int: Int){

        val sicherheitsWert=70

        val image = findViewById<ImageView>(R.id.imageView)
        val speedtoInt = signSpeedNow.toInt()

        if (int==10) {
            if (signSpeedNow.toInt()==0) {
                image.setImageResource(R.drawable.limit10)
                signSpeedNow="$int"
                speedLimitSpeak(10)

            }
            if (abs( speedtoInt-int)<sicherheitsWert){
                signSpeedNow="$int"
                image.setImageResource(R.drawable.limit10)
                speedLimitSpeak(10)
            }
        }
        if (int==20) {
            if (signSpeedNow.toInt()==0){
                image.setImageResource(R.drawable.limit20)
                signSpeedNow="$int"
                speedLimitSpeak(20)
            }
            if (abs( speedtoInt-int)<sicherheitsWert){
                signSpeedNow="$int"
                image.setImageResource(R.drawable.limit20)
                speedLimitSpeak(20)
            }
        }
        if (int==30) {
            if (signSpeedNow.toInt()==0){
                image.setImageResource(R.drawable.limit30)
                signSpeedNow="$int"
                speedLimitSpeak(30)
            }
            if (abs( speedtoInt-int)<sicherheitsWert){
                signSpeedNow="$int"
                image.setImageResource(R.drawable.limit30)
                speedLimitSpeak(30)
            }
        }
        if (int==40) {
            if (signSpeedNow.toInt()==0){
                image.setImageResource(R.drawable.limit40)
                signSpeedNow="$int"
                speedLimitSpeak(40)
            }
            if (abs( speedtoInt-int)<sicherheitsWert){
                signSpeedNow="$int"
                image.setImageResource(R.drawable.limit40)
                speedLimitSpeak(40)
            }
        }
        if (int==50) {
            if (signSpeedNow.toInt()==0){
                image.setImageResource(R.drawable.limit50)
                signSpeedNow="$int"
                speedLimitSpeak(50)
            }
            if (abs( speedtoInt-int)<sicherheitsWert){
                signSpeedNow="$int"
                image.setImageResource(R.drawable.limit50)
                speedLimitSpeak(50)
            }
        }
        if (int==60) {
            if (signSpeedNow.toInt()==0){
                image.setImageResource(R.drawable.limit60)
                signSpeedNow="$int"
                speedLimitSpeak(60)
            }
            if (abs( speedtoInt-int)<sicherheitsWert){
                signSpeedNow="$int"
                image.setImageResource(R.drawable.limit60)
                speedLimitSpeak(60)
            }
        }
        if (int==70) {
            if (signSpeedNow.toInt()==0){
                image.setImageResource(R.drawable.limit70)
                signSpeedNow="$int"
                speedLimitSpeak(70)
            }
            if (abs( speedtoInt-int)<sicherheitsWert){
                signSpeedNow="$int"
                image.setImageResource(R.drawable.limit70)
                speedLimitSpeak(70)
            }
        }
        if (int==80) {
            if (signSpeedNow.toInt()==0){
                image.setImageResource(R.drawable.limit80)
                signSpeedNow="$int"
                speedLimitSpeak(80)
            }
            if (abs( speedtoInt-int)<sicherheitsWert){
                signSpeedNow="$int"
                image.setImageResource(R.drawable.limit80)
                speedLimitSpeak(80)
            }
        }
        if (int==90) {
            if (signSpeedNow.toInt()==0){
                image.setImageResource(R.drawable.limit90)
                signSpeedNow="$int"
                speedLimitSpeak(90)
            }
            if (abs( speedtoInt-int)<sicherheitsWert){
                signSpeedNow="$int"
                image.setImageResource(R.drawable.limit90)
                speedLimitSpeak(90)
            }
        }
        if (int==100) {
            if (signSpeedNow.toInt()==0){
                image.setImageResource(R.drawable.limit100)
                signSpeedNow="$int"
                speedLimitSpeak(100)
            }
            if (abs( speedtoInt-int)<sicherheitsWert){
                signSpeedNow="$int"
                image.setImageResource(R.drawable.limit100)
                speedLimitSpeak(100)
            }
            if(signSpeedNow.toInt()==10){
                signSpeedNow="$int"
                image.setImageResource(R.drawable.limit110)
                speedLimitSpeak(100)
            }
        }
        if (int==110) {
            if (signSpeedNow.toInt()==0){
                image.setImageResource(R.drawable.limit110)
                signSpeedNow="$int"
                speedLimitSpeak(110)
            }
            if (abs( speedtoInt-int)<sicherheitsWert){
                signSpeedNow="$int"
                image.setImageResource(R.drawable.limit110)
                speedLimitSpeak(110)
            }
            if(signSpeedNow.toInt()==10){
                signSpeedNow="$int"
                image.setImageResource(R.drawable.limit110)
                speedLimitSpeak(110)
            }
        }
        if (int==120) {
            if (signSpeedNow.toInt()==0){
                image.setImageResource(R.drawable.limit120)
                signSpeedNow="$int"
                speedLimitSpeak(120)
            }
            if (abs( speedtoInt-int)<sicherheitsWert){
                signSpeedNow="$int"
                image.setImageResource(R.drawable.limit120)
                speedLimitSpeak(120)
            }
            if(signSpeedNow.toInt()==20){
                signSpeedNow="$int"
                image.setImageResource(R.drawable.limit120)
                speedLimitSpeak(120)
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
                compare(img,radius)

            } catch (e: Exception) {
                bm = null
            }
            if (bm != null) {
                val image = InputImage.fromBitmap(bm!!, 0)

                  // zeigt alle label vom Kreisinhalt
                labeler.process(image)
                    .addOnSuccessListener { objRec ->
                        for (i in objRec){
                            Log.i("TEST", i.text.toString())
                        }
                    }

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

    fun speedLimitSpeak(int: Int){
        if(soundIstActive){
            ttsSpeed.speak(
                "$int Kilometer die Stunde",
                TextToSpeech.QUEUE_FLUSH,
                null,
                "Speed Detected"
            )
        }
    }

   private fun sizeSetterFrame(){
        val manager = getSystemService(CAMERA_SERVICE) as CameraManager
        val cameraId = manager.cameraIdList[0]
        val characteristics = manager.getCameraCharacteristics(cameraId)
        val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!

        for (size in map.getOutputSizes(SurfaceTexture::class.java)) {
            if (size.width<1800 && size.width>1200) {
                imgHeight=size.height
                imgWidth=size.width
            }
        }
    }

   private fun speedTextColo(trafficSpeed: Int,speed: Double){
        val text= findViewById<TextView>(R.id.speed)
        if (speed<trafficSpeed) text.setTextColor(Color.GREEN)
        if (speed>trafficSpeed) text.setTextColor(Color.RED)
    }

    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    //
    // Test vergleicher von Bitmap !!!!!!!!!!!!!!!
    //
    // by Robert
    //


    private fun compare(b1: Mat?,radius: Int): Int {
        var percentCompare = 0

        val speed10 = getBitmap(R.drawable.limit10, radius)
        val speed20 = getBitmap(R.drawable.limit20, radius)
        val speed30 = getBitmap(R.drawable.limit30, radius)
        val speed40 = getBitmap(R.drawable.limit40, radius)
        val speed50 = getBitmap(R.drawable.limit50, radius)
        val speed60 = getBitmap(R.drawable.limit60, radius)
        val speed70 = getBitmap(R.drawable.limit70, radius)
        val speed80 = getBitmap(R.drawable.limit80, radius)
        val speed90 = getBitmap(R.drawable.limit90, radius)
        val speed100 = getBitmap(R.drawable.limit100, radius)
        val speed110 = getBitmap(R.drawable.limit110, radius)
        val speed120 = getBitmap(R.drawable.limit120, radius)



        var list = listOf<Bitmap?>(
            speed10,
            speed20,
            speed30,
            speed40,
            speed50,
            speed60,
            speed70,
            speed80,
            speed90,
            speed100,
            speed110,
            speed120
        )
        var isSame = false


        return 10
    }

    private fun getBitmap(drawableRes: Int,radius: Int): Bitmap? {
        val drawable = resources.getDrawable(drawableRes)
        val canvas = Canvas()
        val bitmap = Bitmap.createBitmap(
            abs(radius * 2 + 20),
            abs(radius * 2 + 20),
            Bitmap.Config.ARGB_8888
        )
        canvas.setBitmap(bitmap)
        drawable.setBounds(0, 0, radius, radius)
        drawable.draw(canvas)
        return bitmap
    }

    override fun onInit(status: Int) {
        ttsSpeed.language = Locale.GERMANY
    }

    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    // !!!!!!!!!!!!!!!!!!!!!!!! Test Ende !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

}





