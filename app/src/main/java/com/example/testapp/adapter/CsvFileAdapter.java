package com.example.testapp.adapter;

import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.Arrays;
import java.util.List;

public class CsvFileAdapter extends RecyclerView.Adapter<CsvFileAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(String[] data);
    }

    private List<String[]> csvDataList;
    private OnItemClickListener listener;

    public CsvFileAdapter(List<String[]> csvDataList, OnItemClickListener listener) {
        this.csvDataList = csvDataList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_1, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String[] data = csvDataList.get(position);
        holder.textView.setText(Arrays.toString(data));
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onItemClick(data);
            }
        });
    }

    @Override
    public int getItemCount() {
        return csvDataList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(android.R.id.text1);
        }
    }
}

