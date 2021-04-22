package com.google.ar.sceneform.samples.gltf;

import android.graphics.Canvas;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class trydraw extends AppCompatActivity {
    private myView customCanvas;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.trydraw);

    }

    public void clearCanvas(View v) {
        customCanvas.clearCanvas();
    }
}
