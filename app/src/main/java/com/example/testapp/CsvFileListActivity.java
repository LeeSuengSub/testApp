package com.example.testapp;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.testapp.adapter.CsvFileAdapter;
import com.example.testapp.domain.CsvFile;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class CsvFileListActivity extends AppCompatActivity {

    ViewDataBinding binding;

    private static String path = "/storage/emulated/0/Android/data/com.example.testapp/files";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_csv_list);

        RecyclerView recyclerView = findViewById(R.id.recycler_csv_view);
        if(recyclerView == null) {
            Log.e("SS1234", "RecyclerView is null");
        }else {
            recyclerView.setLayoutManager(new LinearLayoutManager(this));

            // 특정 폴더의 경로를 가져옵니다.
            File dir = new File(path);

            // 해당 디렉토리에 있는 모든 파일을 가져옵니다.
            File[] files = dir.listFiles();

            // 파일 이름을 저장할 리스트를 생성합니다.
            List<CsvFile> csvFiles = new ArrayList<CsvFile>();

            // 모든 파일을 순회하면서 파일 이름을 리스트에 추가합니다.
            for (File file : files) {
                Log.d("SS1234", "file: " + file.getName());
                csvFiles.add(new CsvFile(file.getName()));
            }

            CsvFileAdapter adapter = new CsvFileAdapter(csvFiles);
            recyclerView.setAdapter(adapter);
        }
    }
}
