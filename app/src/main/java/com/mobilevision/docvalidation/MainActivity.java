package com.mobilevision.docvalidation;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.gms.vision.face.Landmark;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Select the validation criteria from the options provided on the screen and apply the selected
 * validation logic in the chosen image document.
 */
public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_PICK_IMAGE = 1002;
    private static final String TAG = "Document Validation";
    FaceDetector detector;
    Bitmap imageBitmap;
    ImageView imageView;
    Button btnChoosePicture;
    Button btnDetectFaces;
    TextView tvStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        detector = new FaceDetector.Builder(this)
                .setTrackingEnabled(false)
                .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                .build();

        InputStream stream = getResources().openRawResource(R.raw.upload_id_card);
        imageBitmap = BitmapFactory.decodeStream(stream);

        tvStatus = (TextView) findViewById(R.id.tv_status);
        tvStatus.setVisibility(View.GONE);

        imageView = (ImageView) findViewById(R.id.image);
        imageView.setImageBitmap(imageBitmap);

        btnDetectFaces = (Button) findViewById(R.id.btn_detect_faces);
        btnDetectFaces.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                detectFaces();
            }
        });

        btnChoosePicture = (Button) findViewById(R.id.btn_choose_picture);
        btnChoosePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                tvStatus.setVisibility(View.GONE);
                if(isPermissionGranted()){
                    pickImage();
                }else{
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.CAMERA}, 1);
                }
            }
        });
    }

    /**
     * Check whether the required user permissions are granted
     * @return boolean
     */
    public boolean isPermissionGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return false;
            } else {
                return true;
            }
        } else {
            return true;
        }

    }

    /**
     * Start the ImagePickerActivity to pick the image
     */
    public void pickImage() {
        startActivityForResult(new Intent(this, ImagePickerActivity.class), REQUEST_PICK_IMAGE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (permissions[0].equals(Manifest.permission.CAMERA) && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            pickImage();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_PICK_IMAGE:
                    String imagePath = data.getStringExtra("image_path");
                    setImage(imagePath);
                    break;
            }
        } else {
            System.out.println("Failed to load image");
        }
    }

    /**
     * Set the image to the preview holder
     * @param imagePath
     */
    private void setImage(String imagePath) {
        imageBitmap = getBitmap(imagePath);
        imageView.setImageBitmap(imageBitmap);
    }

    /**
     * Get bitmap from the image file path.
     * @param path
     * @return
     */
    private Bitmap getBitmap(String path) {

        try {

            File f = new File(path);
            InputStream in = new FileInputStream(path);

            final int IMAGE_MAX_SIZE = 800000; // 1.2MP

            // Decode image size
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(in, null, options);
            in.close();


            int scale = 1;
            while ((options.outWidth * options.outHeight) * (1 / Math.pow(scale, 2)) >
                    IMAGE_MAX_SIZE) {
                scale++;
            }
            Log.d(TAG, "scale = " + scale + ", orig-width: " + options.outWidth + ", orig-height: " + options.outHeight);

            Bitmap resultBitmap = null;
            in = new FileInputStream(path);
            if (scale > 1) {
                scale--;
                // scale to max possible inSampleSize that still yields an image
                // larger than target
                options = new BitmapFactory.Options();
                options.inSampleSize = scale;
                resultBitmap = BitmapFactory.decodeStream(in, null, options);

                // resize to desired dimensions
                int height = resultBitmap.getHeight();
                int width = resultBitmap.getWidth();
                Log.d(TAG, "1th scale operation dimenions - width: " + width + ", height: " + height);

                double y = Math.sqrt(IMAGE_MAX_SIZE
                        / (((double) width) / height));
                double x = (y / height) * width;

                Bitmap scaledBitmap = Bitmap.createScaledBitmap(resultBitmap, (int) x,
                        (int) y, true);
                resultBitmap.recycle();
                resultBitmap = scaledBitmap;

                System.gc();
            } else {
                resultBitmap = BitmapFactory.decodeStream(in);
            }
            in.close();

            Log.d(TAG, "bitmap size - width: " + resultBitmap.getWidth() + ", height: " +
                    resultBitmap.getHeight());
            return resultBitmap;
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Detect the faces in the chosen image.
     */
    private void detectFaces() {

        Bitmap bmp = Bitmap.createBitmap(imageBitmap.getWidth(), imageBitmap.getHeight(), imageBitmap.getConfig());
        Canvas canvas = new Canvas(bmp);
        canvas.drawBitmap(imageBitmap, 0, 0, null);

        Paint paint = new Paint();
        paint.setColor(Color.GREEN);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5);

        Paint landmarkPaint = new Paint();
        landmarkPaint.setColor(Color.RED);
        landmarkPaint.setStyle(Paint.Style.STROKE);
        landmarkPaint.setStrokeWidth(5);


        Frame frame = new Frame.Builder().setBitmap(imageBitmap).build();
        SparseArray<Face> faces = detector.detect(frame);
        if(faces.size() > 0){
            for (int i = 0; i < faces.size(); ++i) {
                Face face = faces.valueAt(i);

                canvas.drawRect(
                        face.getPosition().x,
                        face.getPosition().y,
                        face.getPosition().x + face.getWidth(),
                        face.getPosition().y + face.getHeight(), paint);

                for (Landmark landmark : face.getLandmarks()) {
                    int cx = (int) (landmark.getPosition().x);
                    int cy = (int) (landmark.getPosition().y);
                    canvas.drawCircle(cx, cy, 5, landmarkPaint);
                }
            }

            imageView.setImageBitmap(bmp);
            Toast.makeText(this, faces.size() + " faces detected", Toast.LENGTH_LONG).show();
            tvStatus.setVisibility(View.VISIBLE);
            tvStatus.setBackgroundColor(getResources().getColor(android.R.color.holo_orange_dark));
            if(faces.size() == 1){
                tvStatus.setText("Valid Document");
                tvStatus.setBackgroundColor(getResources().getColor(android.R.color.holo_green_dark));
            }else if(faces.size() > 1){
                tvStatus.setText("Invalid Document. Too many faces.");
            }else{
                tvStatus.setText("Invalid Document. No face found.");
            }
        }
        else {
            Toast.makeText(this, "No faces detected", Toast.LENGTH_LONG).show();
        }
    }
}
