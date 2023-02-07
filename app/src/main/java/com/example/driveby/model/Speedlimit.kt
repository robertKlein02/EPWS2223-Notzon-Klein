package com.example.driveby.model

import java.util.Date

data class Speedlimit(var latitude  :Double,var longitude:Double, var limit:Int,var limitChange:Int , var richtung :String, var date: Date, var typ :Int)
