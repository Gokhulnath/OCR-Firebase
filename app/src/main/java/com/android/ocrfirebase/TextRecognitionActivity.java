package com.android.ocrfirebase;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextRecognitionActivity extends AppCompatActivity {

    ImageView text_recognition_image_view;
    ImageButton bottom_sheet_button;
    TextView tv_result;
    Toolbar toolbar;
    String final_result;
    List<String> states;
    List<String> keyword;
    List<String> scan;
    ContentValues values;
    Uri imageUri;
    static final int REQUEST_IMAGE_CAPTURE = 1;
    private String pictureImagePath = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_text_recognition);

        text_recognition_image_view = findViewById(R.id.text_recognition_image_view);
        bottom_sheet_button = findViewById(R.id.bottom_sheet_button);
        tv_result = findViewById(R.id.tv_result);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        tv_result.setMovementMethod(new ScrollingMovementMethod());
        final_result= " ";
        scan = new ArrayList<String>();
        bottom_sheet_button.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onClick(View view) {
                if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    values = new ContentValues();
                    values.put(MediaStore.Images.Media.TITLE, "New Picture");
                    values.put(MediaStore.Images.Media.DESCRIPTION, "From your Camera");
                    imageUri = getContentResolver().insert(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                    startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
                } else {
                    requestPermissions(new String[]{Manifest.permission.CAMERA}, 100);
                }
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            try {
                Bitmap picture = MediaStore.Images.Media.getBitmap(
                        getContentResolver(), imageUri);
                analyzeImage(picture);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
//            CropImage.ActivityResult result = CropImage.getActivityResult(data);
//
//            if (resultCode == Activity.RESULT_OK) {
//                Uri imageUri = result.getUri();
//                try {
//                    analyzeImage(MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri));
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
//                Toast.makeText(this, "Error occured", Toast.LENGTH_SHORT).show();
//            }
//        }
//    }

    private void analyzeImage(Bitmap image) {
        if (image == null) {
            Toast.makeText(this, "There was some error", Toast.LENGTH_SHORT).show();
            return;
        }

        text_recognition_image_view.setImageBitmap(null);
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
//                        tv_result.setText(final_result);
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
                final_result += line.getText() + "\n";
                scan.add(line.getText());
            }
        }
    }

    private String Extract_data(List<String> d, String dat) throws JSONException {
        Log.d("fuck", d.toString());
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
                "com", "iss", "usa", "No", "Restrictions", "height", ":", "none", "pa", "organ", "donor", "commercial", "cdl", "driver", "MAINSTREET", "CLA", "signature", "donorr", "enhanced");
        ArrayList<String> keywords = new ArrayList<String>(keyword);

        //State
        for (String state : states) {
            for (int i = 0; i < 4; i++) {
                if (data.get(i).toLowerCase().contains(state.toLowerCase())) {
                    json.put("STATE", state.toUpperCase());
                    data.remove(i);
                }
            }
        }

        //Date
        ArrayList<Date> date = new ArrayList<>();
        ArrayList<Date> date2 = new ArrayList<>();
        Matcher mDate = Pattern.compile("[0-9][0-9][/|-][0-9][0-9][/|-][0-9][0-9][0-9][0-9]").matcher(dat);
        SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy");
        SimpleDateFormat format2 = new SimpleDateFormat("MM-dd-yyyy");
        while (mDate.find()) {
            String temp = mDate.group(0);
            try {
                Date date1 = format.parse(temp);
                date.add(date1);

            } catch (ParseException e) {
                e.printStackTrace();
            }
            try {
                Date date1 = format2.parse(temp);
                date2.add(date1);

            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        Collections.sort(date, new SortByDate());
        Collections.sort(date2, new SortByDate());

        if(date.size()>0 && !format2.format(date.get(0)).isEmpty())
        {
            json.put("DOB", format2.format(date.get(0)));

        }
        if(date.size()>1 && !format2.format(date.get(1)).isEmpty())
        {
            json.put("ISSUE", format2.format(date.get(1)));

        }
        if(date.size()>2 && !format2.format(date.get(2)).isEmpty())
        {
            json.put("EXPIRY", format2.format(date.get(2)));

        }

        if(date2.size()>0 && !format2.format(date2.get(0)).isEmpty())
        {
            json.put("DOB", format2.format(date2.get(0)));

        }
        if(date2.size()>1 && !format2.format(date2.get(1)).isEmpty())
        {
            json.put("ISSUE", format2.format(date2.get(1)));

        }
        if(date2.size()>2 && !format2.format(date2.get(date2.size()-1)).isEmpty())
        {
            json.put("EXPIRY", format2.format(date2.get(date2.size()-1)));

        }

        //DRIVER LICENSE NUMBER
        Matcher mDL = Pattern.compile("DLN:? .*(?! )|DL:? .*(?! )|DL NO.? .*(?! )|DLH:? .*(?! )|DL#:?.*(?! )").matcher(dat);
        while (mDL.find()) {
            Log.d("fuck",mDL.group(0));
            if(mDL.group(0).split(":").length!=1){
                if(mDL.group(0).split(":")[1].trim().split(" ").length>1){
                    json.put("DLN", mDL.group(0).split(":")[1].trim().split(" ")[0]);
                }else{
                    json.put("DLN", mDL.group(0).split(":")[1].trim());
                }
            }
            else {
                if(mDL.group(0).split(" ")[1].trim().contains("NO")){
                    json.put("DLN", mDL.group(0).split("NO")[1].split(" ")[1].trim());
                }else{
                    json.put("DLN", mDL.group(0).split(" ")[1].trim());
                }
            }
        }

        //DRIVER LICENSE NUMBER 2
        Matcher mDL2 = Pattern.compile("LIC/. NO/.:? ?.*|LiC/. NO/.:? ?.*|lic/. NO/.:? ?.*").matcher(dat);
        while (mDL2.find()) {
            json.put("DLN", mDL2.group(0).split("NO")[1].trim());
        }

        //DRIVER LICENSE NUMBER 3
        Matcher mDL3 = Pattern.compile("LIC#[a-zA-Z0-9]*|LiC#[a-zA-Z0-9]*|lic#[a-zA-Z0-9]*").matcher(dat);
        while (mDL3.find()) {
            json.put("DLN", mDL3.group(0).split("#")[1].trim());
        }

        //DRIVER LICENSE NUMBER 4
        Matcher mDL4 = Pattern.compile("LIC(?!ENSE)[a-zA-Z0-9]*|LiC(?!ENSE)[a-zA-Z0-9]*|lic(?!ense)[a-zA-Z0-9]*").matcher(dat);
        while (mDL4.find()) {
            json.put("DLN", mDL4.group(0).substring(3).trim());
        }

        //DRIVER LICENSE NUMBER 7
        Matcher mDL7 = Pattern.compile("LIC #: ?[a-zA-Z0-9]*|LiC #: ?[a-zA-Z0-9]*|lic #: ?[a-zA-Z0-9]*").matcher(dat);
        while (mDL7.find()) {
            json.put("DLN", mDL7.group(0).split(":")[1].trim());
        }

        //DRIVER LICENSE NUMBER 5
        Matcher mDL5 = Pattern.compile("S[0-9]{8}|s[0-9]{8}").matcher(dat);
        while (mDL5.find()) {
            json.put("DLN", mDL5.group(0).trim());
        }

        //DRIVER LICENSE NUMBER 6
        Matcher mDL6 = Pattern.compile("E [0-9 ]{15}|e [0-9 ]{15}").matcher(dat);
        while (mDL6.find()) {
            json.put("DLN", mDL6.group(0).trim());
        }

        //DD
        Matcher mDD = Pattern.compile("DD:? .*").matcher(dat);
        while (mDD.find()) {
            Log.d("fuck", mDD.group(0));
            json.put("DD", mDD.group(0).split(" ")[1].trim());
        }

        //TYPe
        Matcher mtype = Pattern.compile("TYPE:? ?.*").matcher(dat);
        while (mtype.find()) {
            json.put("TYPE", mtype.group(0).split(" ")[1].trim());
        }

        //Address
        String address = "";
        for (int j = 0; j < data.size(); j++) {
            Matcher addr = Pattern.compile(",? [A-Z][A-Z] [[0-9][0-9][0-9][0-9][0-9]|[0-9][0-9][0-9][0-9][0-9]-[0-9][0-9][0-9][0-9]]|[A-Z][A-Z] [0-9]{5}-[0-9]{4}").matcher(data.get(j));
            if (addr.find()) {
                Matcher addr1 = Pattern.compile("^[0-9]+").matcher(data.get(j - 1));
                if (addr1.find()) {
                    address = data.get(j - 1).trim() + ", " + data.get(j).trim();
                    data.remove(j - 1);
                    data.remove(j - 1);
                } else {
                    Matcher addr2 = Pattern.compile("^[0-9]+").matcher(data.get(j - 2));
                    if (addr2.find()) {
                        address = data.get(j - 2).trim() + ", " + data.get(j - 1).trim() + ", " + data.get(j).trim();
                        data.remove(j - 1);
                        data.remove(j - 1);
                        data.remove(j - 1);
                    }
                }
                break;
            }
        }
        if(address.length()!=0){
            json.put("ADDRESS", address.substring(1));
        }

        //Height
        Matcher mh = Pattern.compile("[0-9]-[0-9][0-9]|[0-9]'-[0-9][0-9]''|[0-9]'[0-9][0-9]''").matcher(dat);
        while (mh.find()) {
            json.put("HEIGHT", mh.group(0));
        }

        Matcher mh2 = Pattern.compile("Hgt [0-9]{3}|hgt [0-9]{3}").matcher(dat);
        while (mh2.find()) {
            json.put("HEIGHT", mh2.group(0).substring(4));
        }

        //Weight
        Matcher mw = Pattern.compile("WGT:? ?[0-9]*|Wgt:? ?[0-9]*").matcher(dat);
        while (mw.find()) {
            if(mw.group(0).split(":").length!=1){
                json.put("WEIGHT", mw.group(0).split(":")[1].trim()+" lbs");
            }
            else{
                json.put("WEIGHT", mw.group(0).split(" ")[1].trim()+" lbs");
            }
        }

        //Weight
        Matcher mw1 = Pattern.compile("[0-9]* lb|[0-9]* lbs|[0-9]* LBS|[0-9]* LB").matcher(dat);
        while (mw1.find())
            json.put("WEIGHT", mw1.group(0));



        //Sex
        Matcher msex = Pattern.compile("SEX:? [FM]|Sex:? [FM]").matcher(dat);
        while (msex.find()) {
            if(msex.group(0).split(":").length!=1){
                json.put("SEX", msex.group(0).split(":")[1].trim());
            }
            else{
                json.put("SEX", msex.group(0).split(" ")[1].trim());
            }
        }


        //Eye
        Matcher meye = Pattern.compile(" BLU| BLK| BRO| GRY| GRN| HAZ| MAR| MUL| PNK| XXX| BRN| DIC| BR0").matcher(dat);
        while (meye.find()) {
            json.put("EYE", meye.group(0));
        }

        //Restriction
        Matcher mres = Pattern.compile("RESTR:? (NONE|.)|RESTRICTIONS:? (NONE|.)|Restr:? (NONE|.)|Restrictions:? (NONE|.)|REST:? (NONE|.)|RSTR:? (NONE|.)").matcher(dat);
        while (mres.find()) {
            json.put("RESTRICTION", mres.group(0).split(" ")[1].trim());
        }

        //Endorse
        Matcher mend = Pattern.compile("End:? (NONE|.)|ENDORSE:? (NONE|.)|END:? (NONE|.)|Endorse:? (NONE|.)").matcher(dat);
        while (mend.find()) {
            json.put("ENDORSE", mend.group(0).split(" ")[1].trim());
        }

        //Class
        Matcher mclass = Pattern.compile("CLASS:? .|class:? .|Class:? .|Class:?.").matcher(dat);
        while (mclass.find()) {
            String cl = mclass.group(0);
            if (cl.length() > 0) {
                json.put("CLASS", mclass.group(0).split(" ")[1].trim());
            } else {
                json.put("CLASS", "");
            }
        }

        //cleaning data
        for (int j = 0; j < data.size(); j++) {
            Matcher m1 = Pattern.compile("[0-9][0-9][/|-][0-9][0-9][/|-][0-9][0-9][0-9][0-9]").matcher(data.get(j));
            while (m1.find()) {
                data.set(j, "");
            }
            Matcher m2 = Pattern.compile("DLN:? .*").matcher(data.get(j));
            while (m2.find()) {
                data.set(j, "");
            }
            Matcher m3 = Pattern.compile("[0-9]-[0-9][0-9]|[0-9]'-[0-9][0-9]''|[0-9]'[0-9][0-9]''").matcher(data.get(j));
            while (m3.find()) {
                data.set(j, "");
            }
            Matcher m4 = Pattern.compile("SEX:? [FM]|Sex:? [FM]").matcher(data.get(j));
            while (m4.find()) {
                data.set(j, "");
            }
            Matcher m5 = Pattern.compile(" BLU| BLK| BRO| GRY| GRN| HAZ| MAR| MUL| PNK| XXX").matcher(data.get(j));
            while (m5.find()) {
                data.set(j, "");
            }
            Matcher m6 = Pattern.compile("RESTR: .{1,5}|RESTRICTIONS: .{1,5}|Restr: .{1,5}|Restrictions: .{1,5}|End:? .{1,5}|ENDORSE:? .{1,5}|END:? .{1,5}|Endorse:? .{1,5}").matcher(data.get(j));
            while (m6.find()) {
                data.set(j, "");
            }
            Matcher m7 = Pattern.compile("CLASS: .|class: .").matcher(data.get(j));
            while (m7.find()) {
                data.set(j, "");
            }
            Matcher m8 = Pattern.compile("RESTR:? (NONE|.)|RESTRICTIONS:? (NONE|.)|Restr:? (NONE|.)|Restrictions:? (NONE|.)").matcher(data.get(j));
            while (m8.find()) {
                data.set(j, "");
            }
            String temp = data.get(j);
            String end = "";
            for (String key : temp.split("\\W+")) {
                if (!keywords.contains(key.toLowerCase())) {
                    end += key;
                }
            }
            data.set(j, end);
        }

        Log.d("fuck", data.toString());

        //Name
        int flag = -1;
        ArrayList<String> name = new ArrayList<String>();
        String nameData = "";
        String firstName = "";
        String lastName = "";
        for (String line : data) {
            Matcher mname = Pattern.compile("\\b[A-Z]*\\b").matcher(line);
            while (mname.find()) {
                String temp = mname.group(0).trim();
                for (String m : keywords) {
                    if (!temp.toLowerCase().contains(m.toLowerCase())) {
                        name.add(temp);
                        break;
                    }
                }
            }
            Matcher m2name = Pattern.compile("\\b2[A-Z]*\\b").matcher(line);
            while (m2name.find()) {
                name.clear();
                firstName = m2name.group(0).trim().substring(1);
                break;
            }

            Matcher m1name = Pattern.compile("\\b1[A-Z]*\\b").matcher(line);
            while (m1name.find()) {
                name.clear();
                lastName = m1name.group(0).trim().substring(1);
                break;
            }

        }

        if (!firstName.isEmpty() && !lastName.isEmpty()) {
            nameData = firstName + " " + lastName;
        }
        else{
            if(!firstName.isEmpty()){
                if(firstName.charAt(0)=='L' && firstName.charAt(1)=='N' || firstName.charAt(0)=='F' && firstName.charAt(1)=='N')
                {
                    nameData += firstName.substring(2);
                }else {
                    nameData += firstName;
                }
            }
            if(!lastName.isEmpty()){
                if(lastName.charAt(0)=='F' && lastName.charAt(1)=='N' || lastName.charAt(0)=='L' && lastName.charAt(1)=='N')
                {
                    nameData += lastName.substring(2);
                }else {
                    nameData += lastName;
                }
            }
            int i=0;
            for (String n : name) {
                if (n.length() > 2 && n.length() < 15 && !keywords.contains(n) && i<2) {
                    if(n.charAt(0)=='F' && n.charAt(1)=='N' || n.charAt(0)=='L' && n.charAt(1)=='N') {
                        nameData += n.substring(2) + " ";
                    }
                    else{
                        nameData += n + " ";
                    }
                    i++;
                }
            }
        }
        json.put("NAME", nameData);

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(json);


    }


    static class SortByDate implements Comparator<Date> {


        @Override
        public int compare(Date a, Date b) {
            return a.compareTo(b);
        }
    }
}
