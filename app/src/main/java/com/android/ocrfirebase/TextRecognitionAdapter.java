package com.android.ocrfirebase;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;


public class TextRecognitionAdapter extends RecyclerView.Adapter<TextRecognitionAdapter.TextRecognitionNameHolder> {
    ArrayList<TextRecognitionModel> textRecognitionArrayList;
    Context context;

    public TextRecognitionAdapter(ArrayList<TextRecognitionModel> textRecognitionArrayList, Context context) {
        this.textRecognitionArrayList = textRecognitionArrayList;
        this.context = context;
    }

    public ArrayList<TextRecognitionModel> getTextRecognitionArrayList() {
        return textRecognitionArrayList;
    }

    public void setTextRecognitionArrayList(ArrayList<TextRecognitionModel> textRecognitionArrayList) {
        this.textRecognitionArrayList = textRecognitionArrayList;
    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    @NonNull
    @Override
    public TextRecognitionNameHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_text_recognition, parent, false);
        TextRecognitionNameHolder textRecognitionNameHolder = new TextRecognitionNameHolder(v);
        return textRecognitionNameHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull TextRecognitionNameHolder holder, int position) {
        final TextRecognitionModel textRecognitionModel = textRecognitionArrayList.get(position);
        holder.text1.setText(Integer.toString(textRecognitionModel.getId()));
        holder.text2.setText(textRecognitionModel.getText());

    }

    @Override
    public int getItemCount() {
        return textRecognitionArrayList.size();
    }

    public class TextRecognitionNameHolder extends RecyclerView.ViewHolder {

        TextView text1;
        TextView text2;

        public TextRecognitionNameHolder(@NonNull View itemView) {
            super(itemView);
            text1 = itemView.findViewById(R.id.item_text_recognition_text_view1);
            text2 = itemView.findViewById(R.id.item_text_recognition_text_view2);
        }
    }
}
