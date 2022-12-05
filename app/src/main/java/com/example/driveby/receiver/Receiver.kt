package com.example.driveby.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.driveby.Viewmodel
import java.util.*

class Receiver(val viewModel: Viewmodel): BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val speed = Objects.requireNonNull(intent.extras)?.getDouble("speed")

        if (speed != null) {
            println(speed)
            viewModel.setSpeed(speed)
        }

    }
}