package com.google.ar.sceneform.samples.gltf;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

public class trydraw extends AppCompatActivity {
    private static final String TAG ="BRUH";
    private myView customCanvas;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.trydraw);
        customCanvas=new myView(this,null);
        setContentView(customCanvas);
    }

    public void clearCanvas(View v) {
        customCanvas.clearCanvas();
    }
}
