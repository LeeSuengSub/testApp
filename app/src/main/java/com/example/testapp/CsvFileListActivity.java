package com.example.testapp;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.ViewDataBinding;

import com.example.testapp.singleton.Singleton;

public class CsvFileListActivity extends AppCompatActivity {

    ViewDataBinding binding;

    private static String path = "/storage/emulated/0/Android/data/com.example.testapp/files/csv";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = getIntent();
        if (intent != null) {
            String selectedCSVPath = intent.getStringExtra("selected_csv_path");

            // 파일 경로를 TextView에 표시 (디버깅용)
//            TextView textView = findViewById(R.id.text_view);
//            textView.setText(selectedCSVPath);

            // Singleton을 통해 파일 이름 가져오기
            Singleton singleton = Singleton.getInstance();
            String selectedMapFile = singleton.getSelectedCsvFile();
            // selectedMapFile을 사용하여 필요한 작업 수행
        }
    }
}
