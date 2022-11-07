package com.example.driveby

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc


private const val CAMERA_REQUEST_CODE=101

class MainActivity : AppCompatActivity() , LocationListener {

    private lateinit var tvGpsLocation: TextView
    private lateinit var locationManager: LocationManager
    private val locationPermissionCode = 2

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val button: Button = findViewById(R.id.getLocation)
        button.setOnClickListener {
            getLocation()
        }

        setupPermission()




        findViewById<AppCompatButton>(R.id.bt_hello).setOnClickListener(){
            startActivity(Intent(this, DetectorActivity::class.java))
        }
    }

    /*
    *
    * Ab hier Funktionen für den Start
    *
    * */
    // Freigabe anfragen für Kamera
    @RequiresApi(Build.VERSION_CODES.M)
    private fun setupPermission(){
        val permission = ContextCompat.checkSelfPermission(this,
            android.Manifest.permission.CAMERA)

        if (permission!= PackageManager.PERMISSION_GRANTED){
            makeRequest()
        }
    }
    private fun makeRequest() {
        ActivityCompat.requestPermissions(this,
            arrayOf(android.Manifest.permission.CAMERA),
            CAMERA_REQUEST_CODE
        )
    }

    private fun getLocation() {
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if ((ContextCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)) {
            ActivityCompat.requestPermissions(this, arrayOf(ACCESS_FINE_LOCATION), locationPermissionCode)
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 5f, this)
    }
    override fun onLocationChanged(location: Location) {
        tvGpsLocation = findViewById(R.id.textView)
        tvGpsLocation.text = "Latitude: " + location.latitude + " , Longitude: " + location.longitude
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == locationPermissionCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show()
            }
            else {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }
    }


    fun makeGray(bitmap: Bitmap) : Bitmap {

        // Create OpenCV mat object and copy content from bitmap
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)

        // Convert to grayscale
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGB2GRAY)

        // Make a mutable bitmap to copy grayscale image
        val grayBitmap = bitmap.copy(bitmap.config, true)
        Utils.matToBitmap(mat, grayBitmap)

        return grayBitmap
    }


}
