package com.wsy.mp4extractor;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    String mp4Path = "sdcard/glCamera3.mp4";
    ExecutorService executorService = Executors.newFixedThreadPool(1);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                Mp4Decoder mp4Decoder = new Mp4Decoder();
                try {
                    mp4Decoder.init(mp4Path);
                    mp4Decoder.videoDecode();
                    mp4Decoder.release();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

    }

    @Override
    protected void onDestroy() {
        executorService.shutdown();
        super.onDestroy();
    }
}
