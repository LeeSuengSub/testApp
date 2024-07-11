//package com.example.testapp;
//
//import android.content.Intent;
//import android.net.Uri;
//import android.os.Bundle;
//import android.util.Log;
//import android.view.View;
//import android.widget.Button;
//import android.widget.Toast;
//
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.recyclerview.widget.LinearLayoutManager;
//import androidx.recyclerview.widget.RecyclerView;
//
//import com.example.testapp.adapter.CsvAdapter;
//
//import java.io.BufferedReader;
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.InputStreamReader;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.List;
//
//public class FilePickerActivity extends AppCompatActivity {
//
//    private static final int PICK_CSV_FILE_REQUEST_CODE = 1;
//    private RecyclerView recyclerView;
//    private CsvAdapter csvAdapter;
//    private List<String[]> csvDataList = new ArrayList<>();
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_file_picker);
//
//        recyclerView = findViewById(R.id.recyclerView);
//        recyclerView.setLayoutManager(new LinearLayoutManager(this));
//        csvAdapter = new CsvAdapter(csvDataList, new CsvAdapter.OnItemClickListener() {
//            @Override
//            public void onItemClick(String[] data) {
//                // 선택한 항목 데이터 처리
//                String message = "Selected: " + Arrays.toString(data);
//                Toast.makeText(FilePickerActivity.this, message, Toast.LENGTH_SHORT).show();
//            }
//        });
//        recyclerView.setAdapter(csvAdapter);
//
//        Button btnPickCsvFile = findViewById(R.id.btnPickCsvFile);
//        btnPickCsvFile.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                openFilePicker();
//            }
//        });
//    }
//
//    private void openFilePicker() {
//        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
//        intent.addCategory(Intent.CATEGORY_OPENABLE);
//        intent.setType("text/csv");  // CSV 파일만 선택 가능
//        startActivityForResult(intent, PICK_CSV_FILE_REQUEST_CODE);
//    }
//
//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//        if (requestCode == PICK_CSV_FILE_REQUEST_CODE && resultCode == RESULT_OK) {
//            if (data != null) {
//                Uri uri = data.getData();
//                if (uri != null) {
//                    Log.d("FilePicker", "File selected: " + uri.toString());
//                    readCsvFromUri(uri);
//                } else {
//                    Log.e("FilePicker", "No file selected");
//                }
//            } else {
//                Log.e("FilePicker", "Data is null");
//            }
//        }
//    }
//
//    private void readCsvFromUri(Uri uri) {
//        try (InputStream inputStream = getContentResolver().openInputStream(uri);
//             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
//            String line;
//            csvDataList.clear();
//            while ((line = reader.readLine()) != null) {
//                String[] columns = line.split(",");
//                csvDataList.add(columns);
//            }
//            csvAdapter.notifyDataSetChanged();
//        } catch (IOException e) {
//            Log.e("FilePicker", "Error reading file", e);
//        }
//    }
//}
//
