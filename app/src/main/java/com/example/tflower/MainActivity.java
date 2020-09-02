package com.example.tflower;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;


import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    public static int SPLASH_TIME_OUT = 5000;

    /**
     * Requests Codes to identify camera and permission requests
     */
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 1000;
    private static final int CAMERA_REQEUST_CODE = 10001;

    /**
     * UI Elements
     */
    private ImageView imageView;
    private ListView listView;
    private ImageClassifier imageClassifier;
    private Bitmap bitmapImg;
    private FloatingActionButton shareBtn;
    private String prediction;
    //private String imgPath;
    //private TextView description;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // initalizing ui elements
        initializeUIElements();
    }

    /**
     * Method to initalize UI Elements. this method adds the on click
     */
    @SuppressLint("WrongViewCast")
    private void initializeUIElements() {
        imageView = findViewById(R.id.iv_capture);
        listView = findViewById(R.id.tv_probabilities);
        Button takepicture = findViewById(R.id.bt_take_picture);
        //description = findViewById(R.id.tv_Description);
        shareBtn = findViewById(R.id.btn_share);

        /*
         * Creating an instance of our tensor image classifier
         */
        try {
            imageClassifier = new ImageClassifier(this);
        } catch (IOException e) {
            Log.e("Image Classifier Error", "ERROR: " + e);
        }

        // adding on click listener to camera button
        takepicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // checking whether camera permissions are available.
                // if permission is avaialble then we open camera intent to get picture
                // otherwise reqeusts for permissions
                if (hasPermission()) {
                    openCamera();
                } else {
                    requestPermission();
                }
            }
        });

        shareBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //if results where added - image displayed
                if(prediction == null /*| findViewById(R.id.iv_capture) == null*/){
                    Toast.makeText(getBaseContext(), "No data for sharing yet", Toast.LENGTH_SHORT).show();
                }
                else{
                    shareResults(bitmapImg);

                }

            }
        });

    }



    /**
     * checks whether all the needed permissions have been granted or not
     *
     * @param grantResults the permission grant results
     * @return true if all the reqested permission has been granted,
     * otherwise returns false
     */
    private boolean hasAllPermissions(int[] grantResults) {
        for (int result : grantResults) {
            if (result == PackageManager.PERMISSION_DENIED)
                return false;
        }
        return true;
    }

    /**
     * Method requests for permission if the android version is marshmallow or above
     */
    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // whether permission can be requested or on not
            if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                Toast.makeText(this, "Camera Permission Required", Toast.LENGTH_SHORT).show();
            }
            // request the camera permission permission
            requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
        }
    }

    /**
     * checks whether camera permission is available or not
     *
     * @return true if android version is less than marshmallo,
     * otherwise returns whether camera permission has been granted or not
     */
    private boolean hasPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // if this is the result of our camera permission request
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (hasAllPermissions(grantResults)) {
                openCamera();
            } else {
                requestPermission();
            }
        }
    }

    /**
     * creates and starts an intent to get a picture from camera
     */
    private void openCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(cameraIntent, CAMERA_REQEUST_CODE);
    }

    @SuppressLint("SetTextI18n")
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        // if this is the result of our camera image request
        if (requestCode == CAMERA_REQEUST_CODE) {
            // getting bitmap of the image
            Bitmap photo = (Bitmap) Objects.requireNonNull(Objects.requireNonNull(data).getExtras()).get("data");
            //save the image for further share
            bitmapImg = photo;
            // displaying this bitmap in imageview
            imageView.setImageBitmap(photo);

            // pass this bitmap to classifier to make prediction
            List<ImageClassifier.Recognition> predicitons = imageClassifier.recognizeImage(
                    photo, 0);

            // creating a list of string to display in list view
            final List<String> predicitonsList = new ArrayList<>();
            prediction = null;

            for (ImageClassifier.Recognition recog : predicitons) {

                predicitonsList.add("This flower is " + recog.getName() + "  ::::::::  " + (recog.getConfidence() * 100));
                //save results for sharing
                if(prediction == null)
                    prediction = "Prediction of " + recog.getName() + " is: " + (recog.getConfidence() * 100 + "\n");
                else
                    prediction += "Prediction of " + recog.getName() + " is: " + (recog.getConfidence() * 100 + "\n");


/*
                    //Add in formation according to the flower type
                    switch (recog.getName()) {
                        case ("daisy"):
                            description.setText("A Daisy flower is composed of white petals and a yellow center, although the flower can sometimes have a pink or rose color. " +
                                    "See More in Here:https://www.pickupflowers.com/flower-guide/daisies");
                            Linkify.addLinks(description, Linkify.ALL);
                            break;
                        case ("dandelion"):
                            description.setText("Dandelion is generally an apomictic plant and seed production normally occurs without pollination. Seed production varies from 54 to 172 seeds per head and a single plant can produce more than 2000 seeds. One estimate is that more than 240,000,000 seeds/acre could be produced annually by a dense stand of dandelions." +
                                    "See More in Here:https://web.archive.org/web/20081022071446/http://128.104.239.6/uw_weeds/extension/articles/dandelion.htm");
                            Linkify.addLinks(description, Linkify.ALL);
                            break;
                        case ("roses"):
                            description.setText("The rose has been a symbol of love, beauty, even war and politics from way back in time. The variety, color and even number of Roses carry symbolic meanings. The Rose is most popularly known as the flower of love, particularly Red Rose." +
                                    "See More in Here:https://www.pickupflowers.com/flower-guide/rose");
                            Linkify.addLinks(description, Linkify.ALL);
                            break;
                        case ("sunflowers"):
                            description.setText("The large, solitary Sunflower blossom, sometimes as large as a meter in diameter, is composed of yellow ray flowers and a central disk.n The Central disk is composed of either yellow, brown, or purple flowers, depending on the species. The flower is actually a head (formerly composite flower) of numerous flowers crowded together. The outer flowers on the Sunflower are the ray florets and can be yellow, maroon, orange, or other colors. These flowers are sterile. The flowers that fill the circular head inside the ray flowers are called disc florets." +
                                    "See More in Here:https://www.pickupflowers.com/flower-guide/sunflower");
                            Linkify.addLinks(description, Linkify.ALL);
                            break;
                        case ("tulips"):
                            description.setText("Tulips are some of the most popular spring flowers of all time, and the third most popular flowers world-wide next only to the Rose and Chrysanthemum. Tulips come in an incredible variety of colors, height, and flower shapes. Some Tulips are even fragrant." +
                                    "See More in Here:https://www.pickupflowers.com/flower-guide/tulips");
                            Linkify.addLinks(description, Linkify.ALL);
                            break;
                        default:
                            break;

                    }*/

            }

            // creating an array adapter to display the classification result in list view
            ArrayAdapter<String> predictionsAdapter = new ArrayAdapter<>(
                    this, R.layout.support_simple_spinner_dropdown_item, predicitonsList);
            listView.setAdapter(predictionsAdapter);


        }
        super.onActivityResult(requestCode, resultCode, data);

    }



    /**
     * save the image in chace and share it using intent
     */
    private void shareResults(Bitmap bitmap){
        // save bitmap to cache directory
        try {
            File cachePath = new File(this.getCacheDir(), "images");
            cachePath.mkdirs(); // don't forget to make the directory
            FileOutputStream stream = new FileOutputStream(cachePath + "/image.png"); // overwrites this image every time
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
            stream.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
        File imagePath = new File(this.getCacheDir(), "images");
        File newFile = new File(imagePath, "image.png");
        Uri contentUri = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".fileprovider", newFile);

        if (contentUri != null) {
            Intent shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); // temp permission for receiving app to read this file
            shareIntent.setDataAndType(contentUri, getContentResolver().getType(contentUri));
            shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
            shareIntent.setType("image/png");
            shareIntent.putExtra(Intent.EXTRA_TEXT, prediction);
            startActivity(Intent.createChooser(shareIntent, "Choose an app"));


        }
    }
}