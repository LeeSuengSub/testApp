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

import java.util.ArrayList;
import java.util.List;

public class CsvFileListActivity extends AppCompatActivity {

    ViewDataBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_csv_list);

        RecyclerView recyclerView = findViewById(R.id.recycler_csv_view);
        if(recyclerView == null) {
            Log.e("SS1234", "RecyclerView is null");
        }else {
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            List<CsvFile> csvFiles = new ArrayList<>();
            csvFiles.add(new CsvFile("1111"));
            csvFiles.add(new CsvFile("2222"));
            csvFiles.add(new CsvFile("3333"));
            CsvFileAdapter adapter = new CsvFileAdapter(csvFiles);
            recyclerView.setAdapter(adapter);
        }
    }
}
