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
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextRecognitionActivity extends AppCompatActivity {

    ImageView text_recognition_image_view;
    ImageButton bottom_sheet_button;
//    RecyclerView bottom_sheet_recycler_view;
    TextView tv_result;
    Toolbar toolbar;
    TextRecognitionAdapter textRecognitionAdapter;
    ArrayList<TextRecognitionModel> textRecognitionModelArrayList;
    String final_result;
    List<String> states;
    List<String> keyword;
    List<String> scan;
    int CAMERA_PICTURE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_text_recognition);

        text_recognition_image_view = findViewById(R.id.text_recognition_image_view);
        bottom_sheet_button = findViewById(R.id.bottom_sheet_button);
        tv_result = findViewById(R.id.tv_result);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        final_result= " ";
        scan = new ArrayList<String>();
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
//        bottom_sheet_recycler_view.setLayoutManager(linearLayoutManager);
//        bottom_sheet_recycler_view.setAdapter(textRecognitionAdapter);
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
        final_result= " ";
        scan.clear();
        tv_result.setVisibility(View.VISIBLE);
        InputImage inputImage = InputImage.fromBitmap(image, 0);

        TextRecognizer recognizer = TextRecognition.getClient();
        recognizer.process(inputImage)
                .addOnSuccessListener(new OnSuccessListener<Text>() {
                    @Override
                    public void onSuccess(Text text) {
                        Bitmap mutableImage = image.copy(Bitmap.Config.ARGB_8888, true);
                        recognizeText(text, mutableImage);
                        text_recognition_image_view.setImageBitmap(mutableImage);
                        try {
                            tv_result.setText(Extract_data(scan,final_result));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
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
                final_result += line.getText() + "\n";
                scan.add(line.getText());
            }
        }
    }

    private String Extract_data(List<String> d, String dat) throws JSONException {
        ArrayList<String> data = new ArrayList<String>(d);
        JSONObject json = new JSONObject();
        states = Arrays.asList("Alabama", "Alaska", "Arizona", "Arkansas", "California", "Colorado",
                "Connecticut", "Delaware", "Florida", "Georgia", "Hawaii", "Idaho", "Illinois",
                "Indiana", "Iowa", "Kansas", "Kentucky", "Louisiana", "Maine", "Maryland",
                "Massachusetts", "Michigan", "Minnesota", "Mississippi", "Missouri",
                "Montana", "Nebraska", "Nevada", "New Hampshire", "New Jersey", "New Mexico",
                "New York", "North Carolina", "North Dakota", "Ohio", "Oklahoma", "Oregon",
                "Pennsylvania", "Rhode Island", "South Carolina", "South Dakota", "Tennessee",
                "Texas", "Utah", "Vermont", "Virginia", "Washington", "West Virginia", "Wisconsin", "Wyoming");

        keyword = Arrays.asList("pennsylvania", "driver's", "drivers", "driver", "license", "visit", "dln",
                "dob", "exp", "sex", "hgt", "eyes", "class", "end", "restr", "dups", "dd", "dl",
                "com", "iss", "usa", "No", "Restrictions", "height", ":", "none", "pa","organ", "donor","sample","commercial", "cdl", "driver");
        ArrayList<String> keywords = new ArrayList<String>(keyword);

        //State
        for (String state : states) {
            for (int i = 0; i < 4; i++) {
                if (data.get(i).contains(state)) {
                    json.put("STATE", state);
                    data.remove(i);
                }
            }
        }

        //Date
        ArrayList<Date> date = new ArrayList<>();
        Matcher mDate = Pattern.compile("[0-9][0-9][/|-][0-9][0-9][/|-][0-9][0-9][0-9][0-9]").matcher(dat);
        SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy");
        while (mDate.find()) {
            String temp = mDate.group(0);
            try {
                Date date1 = format.parse(temp);
                date.add(date1);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        SimpleDateFormat format2 = new SimpleDateFormat("MM-dd-yyyy");
        json.put("DOB", format2.format(date.get(0)));
        json.put("ISSUE", format2.format(date.get(1)));
        json.put("EXPIRY", format2.format(date.get(2)));

        //DRIVER LICENSE NUMBER
        Matcher mDL = Pattern.compile("DLN:? .*").matcher(dat);
        while (mDL.find()) {
            json.put("DLN", mDL.group(0).split(":")[1].trim());
        }

        //Address
        String address = "";
        for (int j = 0; j < data.size(); j++) {
            Matcher addr = Pattern.compile(", [A-Z][A-Z] [[0-9][0-9][0-9][0-9][0-9]|[0-9][0-9][0-9][0-9][0-9]-[0-9][0-9][0-9][0-9]]").matcher(data.get(j));
            if (addr.find()) {
                Matcher addr1 = Pattern.compile("^[0-9]+").matcher(data.get(j - 1));
                if (addr1.find()) {
                    address = data.get(j - 1) + "\n" + data.get(j);
                    data.remove(j - 1);
                    data.remove(j - 1);
                } else {
                    Matcher addr2 = Pattern.compile("^[0-9]+").matcher(data.get(j - 2));
                    if (addr2.find()) {
                        address = data.get(j - 2) + "\n" + data.get(j - 1) + "\n" + data.get(j);
                        data.remove(j - 1);
                        data.remove(j - 1);
                        data.remove(j - 1);
                    }
                }
                break;
            }
        }
        json.put("ADDRESS", address);

        //Height
        Matcher mh = Pattern.compile("[0-9]-[0-9][0-9]|[0-9]'-[0-9][0-9]''|[0-9]'[0-9][0-9]''").matcher(dat);
        while (mh.find()) {
            json.put("HEIGHT", mh.group(0));
        }

        //Sex
        Matcher msex = Pattern.compile("SEX:? [FM]|Sex:? [FM]").matcher(dat);
        while (msex.find()) {
            json.put("SEX", msex.group(0).split(":")[1].trim());
        }

        //Eye
        Matcher meye = Pattern.compile(" BLU| BLK| BRO| GRY| GRN| HAZ| MAR| MUL| PNK| XXX").matcher(dat);
        while (meye.find()) {
            json.put("EYE", meye.group(0));
        }

        //Restriction
        Matcher mres = Pattern.compile("RESTR:? (NONE|.)|RESTRICTIONS:? (NONE|.)|Restr:? (NONE|.)|Restrictions:? (NONE|.)").matcher(dat);
        while (mres.find()) {
            json.put("RESTRICTION", mres.group(0).split(" ")[1].trim());
        }

        //Endorse
        Matcher mend = Pattern.compile("End:? (NONE|.)|ENDORSE:? (NONE|.)|END:? (NONE|.)|Endorse:? (NONE|.)").matcher(dat);
        while (mend.find()) {
            json.put("ENDORSE", mend.group(0).split(" ")[1].trim());
        }

        //Class
        Matcher mclass = Pattern.compile("CLASS:? .|class:? .").matcher(dat);
        while (mclass.find()) {
            String cl = mclass.group(0);
            if(cl.length()>0) {
                json.put("CLASS", mclass.group(0).split(" ")[1].trim());
            }
            else{
                json.put("CLASS","");
            }
        }

        //cleaning data
        for(int j=0;j<data.size();j++){
            Matcher m1 = Pattern.compile("[0-9][0-9][/|-][0-9][0-9][/|-][0-9][0-9][0-9][0-9]").matcher(data.get(j));
            while (m1.find()) {
                data.set(j,"");
            }
            Matcher m2 = Pattern.compile("DLN:? .*").matcher(data.get(j));
            while (m2.find()) {
                data.set(j,"");
            }
            Matcher m3 = Pattern.compile("[0-9]-[0-9][0-9]|[0-9]'-[0-9][0-9]''|[0-9]'[0-9][0-9]''").matcher(data.get(j));
            while (m3.find()) {
                data.set(j,"");
            }
            Matcher m4 = Pattern.compile("SEX:? [FM]|Sex:? [FM]").matcher(data.get(j));
            while (m4.find()) {
                data.set(j,"");
            }
            Matcher m5 = Pattern.compile(" BLU| BLK| BRO| GRY| GRN| HAZ| MAR| MUL| PNK| XXX").matcher(data.get(j));
            while (m5.find()) {
                data.set(j,"");
            }
            Matcher m6 = Pattern.compile("RESTR: .{1,5}|RESTRICTIONS: .{1,5}|Restr: .{1,5}|Restrictions: .{1,5}|End:? .{1,5}|ENDORSE:? .{1,5}|END:? .{1,5}|Endorse:? .{1,5}").matcher(data.get(j));
            while (m6.find()) {
                data.set(j,"");
            }
            Matcher m7 = Pattern.compile("CLASS: .|class: .").matcher(data.get(j));
            while (m7.find()) {
                data.set(j,"");
            }
            Matcher m8 = Pattern.compile("RESTR:? (NONE|.)|RESTRICTIONS:? (NONE|.)|Restr:? (NONE|.)|Restrictions:? (NONE|.)").matcher(data.get(j));
            while (m8.find()) {
                data.set(j,"");
            }
            String temp = data.get(j);
            String end = "";
            for(String key:temp.split("\\W+")){
                if(!keywords.contains(key.toLowerCase())){
                    end += key;
                }
            }
            data.set(j,end);
        }

        Log.d("fuck",data.toString());

        //Name
        ArrayList<String> name = new ArrayList<String>();
        String nameData="";
        for(String line:data){
            Matcher mname = Pattern.compile("[A-Z]*").matcher(line);
            while (mname.find()) {
                name.add(mname.group(0).trim());
            }
        }
        for(String n:name){
            if(n.length()>2){
                nameData+=n+" ";
            }
        }
        json.put("NAME",nameData);

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(json);

    }
}