package com.example.driveby.view

import android.Manifest
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


private const val CAMERA_PRE=101
private const val GPS_PRE = 2

class MainActivity : AppCompatActivity()  {




    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        if (ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED){
            startActivity(Intent(this, DetectorActivity::class.java))
        }




        findViewById<AppCompatButton>(R.id.bt_hello).setOnClickListener(){
            if(ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA)==PackageManager.PERMISSION_DENIED){
                checkPremission(Manifest.permission.CAMERA,101)
            }
            if(ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_DENIED){
                checkPremission(Manifest.permission.ACCESS_FINE_LOCATION,2)
            }else{
                startActivity(Intent(this, DetectorActivity::class.java))
            }
        }
    }



    /*  * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
    *
    *  Ab hier Funktionen
    *
    * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    fun checkPremission(premisson:String,requestCode:Int){

        if(ContextCompat.checkSelfPermission(this,premisson)==PackageManager.PERMISSION_DENIED){
            ActivityCompat.requestPermissions(this, arrayOf(premisson),requestCode)
        }else{
            Toast.makeText(this,"Permission ist granted",Toast.LENGTH_SHORT).show()
        }

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode== GPS_PRE){
            if (grantResults.isNotEmpty() && grantResults[0]==PackageManager.PERMISSION_GRANTED){
                Toast.makeText(this,"Location permission Granted",Toast.LENGTH_SHORT).show()
                this.recreate()
            }else{
            //    Toast.makeText(this,"Location permission Denied",Toast.LENGTH_SHORT).show()
            }
        }else if(requestCode== CAMERA_PRE){
            if (grantResults.isNotEmpty() && grantResults[0]==PackageManager.PERMISSION_GRANTED){
                Toast.makeText(this,"Camera permission Granted",Toast.LENGTH_SHORT).show()
                this.recreate()
            }else{
          //      Toast.makeText(this,"Camera permission Denied",Toast.LENGTH_SHORT).show()
            }
        }
    }


}
