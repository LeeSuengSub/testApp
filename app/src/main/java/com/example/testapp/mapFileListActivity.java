package com.example.testapp;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.testapp.adapter.MapFileAdapter;
import com.example.testapp.domain.MapFile;

import java.util.ArrayList;
import java.util.List;

public class mapFileListActivity extends AppCompatActivity {

    ViewDataBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_map_list);

        RecyclerView recyclerView = findViewById(R.id.recycler_map_view);
        if(recyclerView == null) {
            Log.e("SS1234", "RecyclerView is null");
        }else {
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            List<MapFile> mapFiles = new ArrayList<>();
            mapFiles.add(new MapFile("Map1"));
            mapFiles.add(new MapFile("Map2"));
            mapFiles.add(new MapFile("Map3"));
            MapFileAdapter adapter = new MapFileAdapter(mapFiles);
            recyclerView.setAdapter(adapter);
        }
    }

}
