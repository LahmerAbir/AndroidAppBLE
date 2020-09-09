package com.yt.BLEClient;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.ImageView;

import com.skyfishjy.library.RippleBackground;

public class MainActivity extends AppCompatActivity {
    Thread timer;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final RippleBackground rippleBackground=(RippleBackground)findViewById(R.id.content);
        final ImageView imageView=(ImageView)findViewById(R.id.centerImage);
        imageView.setColorFilter(Color.argb(255, 255, 255, 255)); //change the logo color while staring animation
        rippleBackground.startRippleAnimation(); //starting the animation
        timer = new Thread(){
            @Override
            public void run() {
                try {
                    synchronized (this){
                        wait(5000);
                        Intent intent = new Intent(MainActivity.this, DeviceScanActivity.class);
                        startActivity(intent);
                    }
                } catch (InterruptedException e){
                    e.printStackTrace();
                } finally {

                }
            }
        };
        timer.start();


    }
}
