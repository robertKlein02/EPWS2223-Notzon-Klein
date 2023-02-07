package com.example.driveby

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.driveby.model.Speedlimit
import java.util.*

class ViewmodelSpeedLi:ViewModel() {

    inner class Receiver(): BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val speed = Objects.requireNonNull(intent.extras)?.getDouble("speed")

            if (speed != null) {
                println(speed)
                setSpeed(speed)
            }
        }
    }

    private val _speed : MutableLiveData<Double> = MutableLiveData<Double>()
    var speed : MutableLiveData<Double> = _speed

    var listeVonSpeedlimit=MutableLiveData<Speedlimit>()

    init {
        setSpeed(0.0)
    }

    fun setSpeed(double: Double){
        _speed.postValue(double)
    }
}