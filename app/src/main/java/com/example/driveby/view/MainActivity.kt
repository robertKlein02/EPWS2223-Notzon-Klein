package com.example.driveby.view

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
import androidx.lifecycle.ViewModel
import com.example.driveby.R

import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc


private const val CAMERA_REQUEST_CODE=101

class MainActivity : AppCompatActivity()  {



    private lateinit var tvGpsLocation: TextView
    private lateinit var locationManager: LocationManager
    private val locationPermissionCode = 2

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        getLocation()
        setupPermission()




        findViewById<AppCompatButton>(R.id.bt_hello).setOnClickListener(){
            startActivity(Intent(this, DetectorActivity::class.java))
        }
    }


    /*  * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
    *
    *  Ab hier Funktionen
    *
    * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */


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


    // Freigabe für Location and start LocationListener
    private fun getLocation() {
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if ((ContextCompat.checkSelfPermission(
                this,
                ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED)
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(ACCESS_FINE_LOCATION),
                locationPermissionCode
            )
        }
    }



}
