package com.sardinecorp.nocameraintent;

import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback {

    /*
     * This is an example project of how to implement a camera surface view
     * that is hidden to the user. When we click the button on activity_main.xml a picture
     * is taken with the front-facing camera and stored on the device
     *
     * Credits:
     * https://developer.android.com/training/permissions/requesting.html
     * http://www.androidcodec.com/android-surfaceview-example/
     */

    @SuppressWarnings("deprecation")
    private android.hardware.Camera camera;
    private SurfaceHolder surfaceHolder;
    private boolean camCondition = false;
    private Button mCaptureButton;
    private SurfaceView mCameraPreview;

    // Permissions for reading and writing data on android
    private static String[] PERMISSIONS_STORAGE = {
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.CAMERA
    };

    /* Change the type of camera
     * Camera.CameraInfo.CAMERA_FACING_FRONT - front facing camera
     * Camera.CameraInfo.CAMERA_FACING_BACK - back facing camera
     */
    private final int mTypeOfCamera = Camera.CameraInfo.CAMERA_FACING_FRONT;
    // folder name that we are going to save pictures
    private final String FOLDER_NAME = "SecretPic";
    private static final int PERMISSION_REQUEST_STORAGE = 1;

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mCameraPreview = (SurfaceView) findViewById(R.id.cameraPreview);
        mCaptureButton = (Button) findViewById(R.id.pictureButton);


        /*
         * When we start the app we must make sure that we have all the permissions
         * to be able to read and write data, and have access to the camera
         * Check https://developer.android.com/training/permissions/requesting.html for a more
         * detailed explanation of permission request
         */
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                android.Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(MainActivity.this,
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(MainActivity.this,
                        android.Manifest.permission.READ_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED)   {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    android.Manifest.permission.CAMERA)) {

                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(MainActivity.this, PERMISSIONS_STORAGE, PERMISSION_REQUEST_STORAGE);
                // PERMISSION_REQUEST_STORAGE is an
                // app-defined int constant. The callback method gets the
                // result of the request.



            }

        }


        /*
         * Credits: http://www.androidcodec.com/android-surfaceview-example/
         */

        // getWindow() to get window and set it's pixel format which is UNKNOWN
        getWindow().setFormat(PixelFormat.UNKNOWN);
        // getting access to the surface of surfaceView and return it to surfaceHolder
        surfaceHolder = mCameraPreview.getHolder();
        // adding call back to this context means MainActivity
        surfaceHolder.addCallback(MainActivity.this);
        // to set surface type
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_NORMAL);

        mCaptureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                camera.takePicture(null, null, null, mPictureCallback);
            }
        });

    }

    // camera picture taken image and store in directory
    @SuppressWarnings("deprecation")
    android.hardware.Camera.PictureCallback mPictureCallback = new android.hardware.Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] bytes, android.hardware.Camera camera) {
            FileOutputStream outStream = null;
            // Directory and name of the photo. We put system time
            // as a postfix, so all photos will have a unique file name.
            try {
                //folder that you have selected exists, if not, create it.
                String filePath = Environment.getExternalStorageDirectory().getAbsolutePath()+"/"+FOLDER_NAME+"/";
                File newDir = new File (filePath);
                if (!newDir.exists()) {
                    newDir.mkdirs();
                }
                String filename = ""+System.currentTimeMillis();
                File out = new File(filePath+filename+".png");

                outStream = new FileOutputStream(out);

                Bitmap photo = BitmapFactory.decodeByteArray(bytes,0,bytes.length);
                photo = rotateBitmap(photo);
                photo.compress(Bitmap.CompressFormat.PNG, 100, outStream);
                outStream.close();

                Toast.makeText(MainActivity.this, "Image Taken!", Toast.LENGTH_SHORT).show();


                /** FIREBASE EXAMPLE **/
                /*
                 * If you want to push this information to a Firebase Database,
                 * you can do so by converting the image to an encoded bitmap
                 * The commented code is an example of this.
                 */
                /*
                DatabaseReference pushKey = mDatabase.child("requests").child(filename);
                // set the name of the file
                pushKey.child("name").setValue(requestName);
                // set the time so that you can order the files on the firebase database
                newTime = time;
                Log.d("time", "SECOND-PHASE "+difference);
                //  set the image data
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                Bitmap photoExt = BitmapFactory.decodeByteArray(bytes,0,bytes.length);
                photoExt = rotateBitmap(photoExt);
                photoExt.compress(Bitmap.CompressFormat.PNG, 10, baos);
                String imageEncoded = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
                pushKey.child("imageURL").setValue(imageEncoded);
                */

                // this is used so that we can reuse the button over and over again
                MainActivity.this.recreate();

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {

            }
        }
    };

    // this code is used if you are uploading the picture and want to change its orientation
    private Bitmap rotateBitmap(Bitmap photo) {
        Matrix rotateRight = new Matrix();
        rotateRight.preRotate(90);

        float[] mirrorY = { -1, 0, 0, 0, 1, 0, 0, 0, 1};
        rotateRight = new Matrix();
        Matrix matrixMirrorY = new Matrix();
        matrixMirrorY.setValues(mirrorY);

        rotateRight.postConcat(matrixMirrorY);

        rotateRight.preRotate(270);

        final Bitmap rImg= Bitmap.createBitmap(photo, 0, 0,
                photo.getWidth(), photo.getHeight(), rotateRight, true);
        return rImg;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay!

                    // this is used so that the app does not crash on the first start
                    MainActivity.this.recreate();

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }


    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {
        // stop the camera
        if(camCondition){
            camera.stopPreview(); // stop preview using stopPreview() method
            camCondition = false; // setting camera condition to false means stop
        }
        // condition to check whether your device have camera or not
        if (camera != null){
            try {
                android.hardware.Camera.Parameters parameters = camera.getParameters();
                camera.setParameters(parameters); // setting camera parameters
                camera.setPreviewDisplay(surfaceHolder); // setting preview of camera
                camera.startPreview();  // starting camera preview

                camCondition = true; // setting camera to true which means having camera
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            // OPEN FRONT FACING CAMERA
            camera = android.hardware.Camera.open(mTypeOfCamera);
            camera.setDisplayOrientation(90);
        } catch (Exception e) {
            Log.e("camera failed", "camera failed");
        }
        // setting camera preview orientation
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (camera != null) {
            camera.stopPreview();  // stopping camera preview
            camera.release();       // releasing camera
            camera = null;          // setting camera to null when left
            camCondition = false;   // setting camera condition to false also when exit from application
        }
    }

}
