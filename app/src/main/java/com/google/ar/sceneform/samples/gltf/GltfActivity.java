/*
 * Copyright 2018 Google LLC. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.ar.sceneform.samples.gltf;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.net.Uri;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.ArraySet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.filament.gltfio.Animator;
import com.google.android.filament.gltfio.FilamentAsset;
import com.google.ar.core.Anchor;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.exceptions.NotYetAvailableException;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.Color;
import com.google.ar.sceneform.rendering.Material;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.Renderable;
import com.google.ar.sceneform.rendering.ViewRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

import java.io.ByteArrayOutputStream;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * This is an example activity that uses the Sceneform UX package to make common AR tasks easier.
 */
public class GltfActivity extends AppCompatActivity {
  private static final String TAG = GltfActivity.class.getSimpleName();
  private static final double MIN_OPENGL_VERSION = 3.0;

  public ArFragment arFragment;
  private Renderable renderable;

  private static class AnimationInstance {
    Animator animator;
    Long startTime;
    float duration;
    int index;

    AnimationInstance(Animator animator, int index, Long startTime) {
      this.animator = animator;
      this.startTime = startTime;
      this.duration = animator.getAnimationDuration(index);
      this.index = index;
    }
  }

  private final Set<AnimationInstance> animators = new ArraySet<>();

  private final List<Color> colors =
      Arrays.asList(
          new Color(0, 0, 0, 1),
          new Color(1, 0, 0, 1),
          new Color(0, 1, 0, 1),
          new Color(0, 0, 1, 1),
          new Color(1, 1, 0, 1),
          new Color(0, 1, 1, 1),
          new Color(1, 0, 1, 1),
          new Color(1, 1, 1, 1));
  private int nextColor = 0;

  @Override
  @SuppressWarnings({"AndroidApiChecker", "FutureReturnValueIgnored"})
  // CompletableFuture requires api level 24
  // FutureReturnValueIgnored is not valid
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (!checkIsSupportedDeviceOrFinish(this)) {
      return;
    }

    setContentView(R.layout.activity_ux);
    arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);
    Button btnn=(Button)findViewById(R.id.btn1);
    btnn.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            try {
                tester();
            } catch (NotYetAvailableException e) {
                e.printStackTrace();
            }
        }
    });
      WeakReference<GltfActivity> weakActivity = new WeakReference<>(this);

    ModelRenderable.builder()
        .setSource(
            this,
            Uri.parse(
                "https://storage.googleapis.com/ar-answers-in-search-models/static/Tiger/model.glb"))
        .setIsFilamentGltf(true)
        .build()
        .thenAccept(
            modelRenderable -> {
              GltfActivity activity = weakActivity.get();
              if (activity != null) {
                activity.renderable = modelRenderable;
              }
            })
        .exceptionally(
            throwable -> {
              Toast toast =
                  Toast.makeText(this, "Unable to load Tiger renderable", Toast.LENGTH_LONG);
              toast.setGravity(Gravity.CENTER, 0, 0);
              toast.show();
              return null;
            });

    arFragment.setOnTapArPlaneListener(
        (HitResult hitResult, Plane plane, MotionEvent motionEvent) -> {
          if (renderable == null) {
            return;
          }

          // Create the Anchor.
          Anchor anchor = hitResult.createAnchor();
          AnchorNode anchorNode = new AnchorNode(anchor);
          anchorNode.setParent(arFragment.getArSceneView().getScene());

          // Create the transformable model and add it to the anchor.
          TransformableNode model = new TransformableNode(arFragment.getTransformationSystem());
          model.setParent(anchorNode);
          model.setRenderable(renderable);
          model.select();

          FilamentAsset filamentAsset = model.getRenderableInstance().getFilamentAsset();
          if (filamentAsset.getAnimator().getAnimationCount() > 0) {
            animators.add(new AnimationInstance(filamentAsset.getAnimator(), 0, System.nanoTime()));
          }

          Color color = colors.get(nextColor);
          nextColor++;
          for (int i = 0; i < renderable.getSubmeshCount(); ++i) {
            Material material = renderable.getMaterial(i);
            material.setFloat4("baseColorFactor", color);
          }

          Node tigerTitleNode = new Node();
          tigerTitleNode.setParent(model);
          tigerTitleNode.setEnabled(false);
          tigerTitleNode.setLocalPosition(new Vector3(0.0f, 1.0f, 0.0f));
          ViewRenderable.builder()
                  .setView(this, R.layout.tiger_card_view)
                  .build()
                  .thenAccept(
                          (renderable) -> {
                              tigerTitleNode.setRenderable(renderable);
                              tigerTitleNode.setEnabled(true);
                          })
                  .exceptionally(
                          (throwable) -> {
                              throw new AssertionError("Could not load card view.", throwable);
                          }
                  );
        });

    arFragment
        .getArSceneView()
        .getScene()
        .addOnUpdateListener(
            frameTime -> {
              Long time = System.nanoTime();
              for (AnimationInstance animator : animators) {
                animator.animator.applyAnimation(
                    animator.index,
                    (float) ((time - animator.startTime) / (double) SECONDS.toNanos(1))
                        % animator.duration);
                animator.animator.updateBoneMatrices();
              }
            });
  }

  /**
   * Returns false and displays an error message if Sceneform can not run, true if Sceneform can run
   * on this device.
   *
   * <p>Sceneform requires Android N on the device as well as OpenGL 3.0 capabilities.
   *
   * <p>Finishes the activity if Sceneform can not run
   */
  public static boolean checkIsSupportedDeviceOrFinish(final Activity activity) {
    if (Build.VERSION.SDK_INT < VERSION_CODES.N) {
      Log.e(TAG, "Sceneform requires Android N or later");
      Toast.makeText(activity, "Sceneform requires Android N or later", Toast.LENGTH_LONG).show();
      activity.finish();
      return false;
    }
    String openGlVersionString =
        ((ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE))
            .getDeviceConfigurationInfo()
            .getGlEsVersion();
    if (Double.parseDouble(openGlVersionString) < MIN_OPENGL_VERSION) {
      Log.e(TAG, "Sceneform requires OpenGL ES 3.0 later");
      Toast.makeText(activity, "Sceneform requires OpenGL ES 3.0 or later", Toast.LENGTH_LONG)
          .show();
      activity.finish();
      return false;
    }
    return true;
  }
    public Bitmap convertYuvImageToRgb(YuvImage yuvImage, int width, int height, int downSample){
        //downSample通常是1-4之間 壓縮圖檔
        try{
            Bitmap rgbImage;
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            yuvImage.compressToJpeg(new Rect(0, 0, width, height), 0, out);
            byte[] imageBytes = out.toByteArray();

            BitmapFactory.Options opt;
            opt = new BitmapFactory.Options();

            opt.inSampleSize = downSample;

            // get image and rotate it so (0,0) is in the bottom left
            Bitmap tmpImage;
            Matrix matrix = new Matrix();
            matrix.postRotate(90); // to rotate the camera images so (0,0) is in the bottom left
            tmpImage = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length, opt);
            rgbImage=Bitmap.createBitmap(tmpImage , 0, 0, tmpImage.getWidth(), tmpImage.getHeight(), matrix, true);

            return rgbImage;}
        catch (IllegalArgumentException e){
            Log.d("Bitmap","illegal");
            return null;

        }
    }

  public void tester() throws NotYetAvailableException {
      Frame frame = arFragment.getArSceneView().getArFrame();
      Image img = frame.acquireCameraImage();
      ByteBuffer ib = ByteBuffer.allocate(img.getHeight() * img.getWidth() * 2);
      ByteBuffer y = img.getPlanes()[0].getBuffer();
      ByteBuffer cr = img.getPlanes()[1].getBuffer();
      ByteBuffer cb = img.getPlanes()[2].getBuffer();
      ib.put(y);
      ib.put(cb);
      ib.put(cr);
      YuvImage yuvImage = new YuvImage(ib.array(),
              ImageFormat.NV21, img.getWidth(), img.getHeight(), null);
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      yuvImage.compressToJpeg(new Rect(0, 0,
              img.getWidth(), img.getHeight()), 50, out);
      byte[] imageBytes = out.toByteArray();
      Log.d(TAG, "tester: "+out.getClass());
      //byte*matrix
      Bitmap bm = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
      //New RGB image
      Bitmap bm2=convertYuvImageToRgb(yuvImage,img.getWidth(),img.getHeight(),1);
      //
      DrawBitmap.bm=bm2;
      Log.d(TAG, "tester: "+bm.getWidth()+bm.getHeight());
      Intent intent=new Intent(this,trydraw.class);
      startActivity(intent);
  }
}
