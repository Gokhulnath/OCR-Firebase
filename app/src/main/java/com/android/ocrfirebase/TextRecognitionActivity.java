package com.android.ocrfirebase;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.IOException;
import java.util.ArrayList;

public class TextRecognitionActivity extends AppCompatActivity {

    ImageView text_recognition_image_view;
    ImageButton bottom_sheet_button;
    RecyclerView bottom_sheet_recycler_view;
    Toolbar toolbar;
    TextRecognitionAdapter textRecognitionAdapter;
    ArrayList<TextRecognitionModel> textRecognitionModelArrayList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_text_recognition);

        text_recognition_image_view = findViewById(R.id.text_recognition_image_view);
        bottom_sheet_button = findViewById(R.id.bottom_sheet_button);
        bottom_sheet_recycler_view = findViewById(R.id.bottom_sheet_recycler_view);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        bottom_sheet_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CropImage.activity()
                        .setGuidelines(CropImageView.Guidelines.ON)
                        .start(TextRecognitionActivity.this);
            }
        });
        textRecognitionModelArrayList = new ArrayList<TextRecognitionModel>();
        textRecognitionAdapter = new TextRecognitionAdapter(new ArrayList<>(), this);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(RecyclerView.VERTICAL);
        bottom_sheet_recycler_view.setLayoutManager(linearLayoutManager);
        bottom_sheet_recycler_view.setAdapter(textRecognitionAdapter);
        textRecognitionAdapter.setTextRecognitionArrayList(textRecognitionModelArrayList);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);

            if (resultCode == Activity.RESULT_OK) {
                Uri imageUri = result.getUri();
                try {
                    analyzeImage(MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Toast.makeText(this, "Error occured", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void analyzeImage(Bitmap image) {
        if (image == null) {
            Toast.makeText(this, "There was some error", Toast.LENGTH_SHORT).show();
            return;
        }

        text_recognition_image_view.setImageBitmap(null);
        textRecognitionModelArrayList.clear();
        textRecognitionAdapter.notifyDataSetChanged();
        bottom_sheet_recycler_view.setVisibility(View.VISIBLE);
        InputImage inputImage = InputImage.fromBitmap(image, 0);

        TextRecognizer recognizer = TextRecognition.getClient();
        recognizer.process(inputImage)
                .addOnSuccessListener(new OnSuccessListener<Text>() {
                    @Override
                    public void onSuccess(Text text) {
                        Bitmap mutableImage = image.copy(Bitmap.Config.ARGB_8888, true);
                        recognizeText(text, mutableImage);
                        text_recognition_image_view.setImageBitmap(mutableImage);
                        textRecognitionAdapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(TextRecognitionActivity.this, "There was some error", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void recognizeText(Text result, Bitmap image) {
        if (result == null || image == null) {
            Toast.makeText(TextRecognitionActivity.this, "There was some error", Toast.LENGTH_SHORT).show();
            return;
        }

        Canvas canvas = new Canvas(image);
        Paint rectPaint = new Paint();
        rectPaint.setColor(Color.RED);
        rectPaint.setStyle(Paint.Style.STROKE);
        rectPaint.setStrokeWidth(4F);
        Paint textPaint = new Paint();
        textPaint.setColor(Color.RED);
        textPaint.setTextSize(40F);

        int index = 0;
        for (Text.TextBlock block : result.getTextBlocks()) {
            for (Text.Line line : block.getLines()) {
                canvas.drawRect(line.getBoundingBox(), rectPaint);
                canvas.drawText(Integer.toString(index), line.getCornerPoints()[2].x, line.getCornerPoints()[2].y, textPaint);
                textRecognitionModelArrayList.add(new TextRecognitionModel(index++, line.getText()));
            }
        }
    }
}