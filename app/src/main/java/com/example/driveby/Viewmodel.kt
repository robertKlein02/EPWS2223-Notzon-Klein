package com.example.driveby

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.driveby.model.Speedlimit

class Viewmodel:ViewModel() {

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