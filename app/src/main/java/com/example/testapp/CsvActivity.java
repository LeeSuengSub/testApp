package com.example.testapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.testapp.adapter.CsvAdapter;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

public class CsvActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private CsvAdapter csvAdapter;
    private List<String> csvPaths = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_csv);

        recyclerView = findViewById(R.id.recycler_csv_view);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 1));
        csvAdapter = new CsvAdapter(this, csvPaths);
        recyclerView.setAdapter(csvAdapter);

        String directoryPath = "/storage/emulated/0/Download/nfcTag";
        File directory = new File(directoryPath);

        if (!directory.exists()) {
            directory.mkdirs();
        }

        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);

        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CsvActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });

        // 다운로드 폴더 경로 설정
        String downloadFolderPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() +"/nfcTag";

        // 다운로드 폴더에서 CSV 파일 로드
        loadCsvFilesFromDirectory(downloadFolderPath);

    }

    private void loadCsvFilesFromDirectory(String directoryPath) {
        File directory = new File(directoryPath);
        File[] files = directory.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".csv");
            }
        });

        if (files != null) {
            for (File file : files) {
                String csvPath = file.getAbsolutePath();
                csvPaths.add(csvPath);
            }
            csvAdapter.notifyDataSetChanged();
        } else {
            Toast.makeText(this, "No .csv files found in folder", Toast.LENGTH_SHORT).show();
        }
    }
}
