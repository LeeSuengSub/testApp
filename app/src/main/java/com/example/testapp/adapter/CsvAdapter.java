package com.example.testapp.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.testapp.MainActivity;
import com.example.testapp.R;
import com.example.testapp.singleton.Singleton;

import java.util.List;

public class CsvAdapter extends RecyclerView.Adapter<CsvAdapter.CsvViewHolder> {

    private List<String> csvPaths;
    private Context context;

    public CsvAdapter(Context context, List<String> csvPaths) {
        this.context = context;
        this.csvPaths = csvPaths;
    }

    @NonNull
    @Override
    public CsvViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_csv, parent, false);
        return new CsvViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CsvViewHolder holder, int position) {
        final String csvPath = csvPaths.get(position);
        holder.fileNameTextView.setText(getFileNameFromPath(csvPath));

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // .csv 파일 경로를 MainActivity로 전달
                Intent intent = new Intent(context, MainActivity.class);
                intent.putExtra("selected_csv_path", csvPath);

                // Singleton을 사용하여 선택된 파일 이름 저장
                String[] str = csvPath.split("/");
                Singleton singleton = Singleton.getInstance();
                singleton.setSelectedCsvFile(str[str.length -1]);
//                singleton.setSelectedCsvFile(csvPath);
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return csvPaths.size();
    }

    public static class CsvViewHolder extends RecyclerView.ViewHolder {

        TextView fileNameTextView;

        public CsvViewHolder(@NonNull View itemView) {
            super(itemView);
            fileNameTextView = itemView.findViewById(R.id.text_file_name);
        }
    }

    private String getFileNameFromPath(String filePath) {
        int lastIndex = filePath.lastIndexOf("/");
        if (lastIndex != -1) {
            return filePath.substring(lastIndex + 1);
        }
        return filePath;
    }
}