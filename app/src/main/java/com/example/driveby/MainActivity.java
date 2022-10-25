package com.example.driveby;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.example.opencvlearning.demo.DetectorActivity;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private AppCompatButton helloButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        helloButton = findViewById(R.id.bt_hello);
        helloButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.bt_hello:
                startActivity(new Intent(this, DetectorActivity.class));
                break;
        }
    }
}