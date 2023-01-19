package com.example.driveby.sensor

import android.app.Service
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlin.math.floor
import kotlin.math.roundToInt



class SpeedSensor:Service() {

    private var locationManager: LocationManager? = null



    inner class LocationListenerKlasse internal constructor(provider: String) :LocationListener{

        private var thisTime: Long = 0
        private var lastTime: Long = 0
        private var speed = 0.0
        private var lastLocation: Location= Location(provider)
        private var distance = 0.0


        override fun onLocationChanged(location: Location) {


            thisTime = System.currentTimeMillis()


            distance = (location.distanceTo(lastLocation) / 1000).toDouble()  // 1000 für M in KM



            var timeDiff = ((thisTime - lastTime) / 1000.0f).toDouble()  // 1000 für milli in sekunden

            timeDiff = (timeDiff * 100.0).roundToInt() / 100.0 // für genauere Werte

            Log.i(TAG, "onLocationChanged: ZEITDIFF: $timeDiff   DISTANZ: $distance")


            speed =floor(distance / timeDiff * 3600 * 10) / 10 




            lastLocation.set(location)
            lastTime = System.currentTimeMillis()



            val intent = Intent("testSpeed")
            intent.putExtra("speed", speed)
            LocalBroadcastManager.getInstance(this@SpeedSensor).sendBroadcast(intent)


        }

    }
    private var locationListeners = arrayOf(
        LocationListenerKlasse(LocationManager.GPS_PROVIDER),
        LocationListenerKlasse(LocationManager.NETWORK_PROVIDER)
    )

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Toast.makeText(this, "Start: Speedsensor", Toast.LENGTH_SHORT).show()
        return START_STICKY
    }


    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        initializeLocationManager()

        try {

            locationManager!!.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                LOCATION_INTERVAL.toLong(),
                LOCATION_DISTANCE.toFloat(),
                locationListeners[0]
            )
            locationManager!!.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                LOCATION_INTERVAL.toLong(),
                LOCATION_DISTANCE.toFloat(),
                locationListeners[1]

            )



        } catch (ex: SecurityException) {
            Log.i(TAG, "Location anfrage nicht möglich", ex)
        }
    }


    private fun initializeLocationManager() {
        Log.e(
            TAG,
            "initializeLocationManager - LOCATION_INTERVAL: $LOCATION_INTERVAL LOCATION_DISTANCE: $LOCATION_DISTANCE"
        )
        if (locationManager == null) {
            locationManager =
                applicationContext.getSystemService(LOCATION_SERVICE) as LocationManager
        }
    }




    override fun onDestroy() {
        Toast.makeText(this, "Stop: Speedsensor", Toast.LENGTH_SHORT).show()
        super.onDestroy()
        if (locationManager != null) {
            for (mLocationListener in locationListeners) {

                try {

                    locationManager!!.removeUpdates(mLocationListener)
                } catch (ex: Exception) {
                    Log.i(TAG, "fail to remove location listener, ignore", ex)
                }
            }
        }
    }


    companion object {
        private const val TAG = "LocationService"
        private const val LOCATION_INTERVAL = 1500
        private const val LOCATION_DISTANCE = 0
    }

}
