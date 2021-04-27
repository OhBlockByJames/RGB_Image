package com.google.ar.sceneform.samples.gltf;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.view.View;

public class myView extends View {
    public myView(Context context, Object o) {
        super(context);

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawBitmap(DrawBitmap.bm,0,0,null);
        //Turn and resize bitmap.
    }
    public void clearCanvas() {

    }

}
