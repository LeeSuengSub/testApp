package com.example.testapp;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.testapp.adapter.MapFileAdapter;
import com.example.testapp.domain.MapFile;
import com.example.testapp.singleton.Singleton;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class mapFileListActivity extends AppCompatActivity {

    ViewDataBinding binding;
    private MapFileAdapter mapFileAdapter;
    private static String path = "/storage/emulated/0/Android/data/com.example.testapp/files/map/";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_map_list);
        RecyclerView recyclerView = findViewById(R.id.recycler_map_view);

        File dir = new File(path);
        if (!dir.exists()) {
            boolean result = dir.mkdirs();
            if (result) {
                Log.d("Folder created", dir.getAbsolutePath());
            } else {
                Log.e("Folder not created", dir.getAbsolutePath());
            }
        }

        if(recyclerView == null) {
            Log.e("SS1234", "RecyclerView is null");
        }else {
            recyclerView.setLayoutManager(new GridLayoutManager(this, 3));

            // 특정 폴더의 경로를 가져옵니다.
            dir = new File(path);
            Log.d("SS1234", "dir: " + dir.getAbsolutePath());

            // 해당 디렉토리에 있는 모든 파일을 가져옵니다.
            File[] files = dir.listFiles();
            Log.d("SS1234", "files: " + files.length);

            // 파일 이름을 저장할 리스트를 생성합니다.
            List<MapFile> mapFiles = new ArrayList<MapFile>();

            // 모든 파일을 순회하면서 파일 이름을 리스트에 추가합니다.
            for (File file : files) {
                Log.d("SS1234", "file: " + file.getName());
                mapFiles.add(new MapFile(file.getName()));
            }

            MapFileAdapter adapter = new MapFileAdapter(mapFiles);
            recyclerView.setAdapter(adapter);

            adapter.setOnItemClickListener(new MapFileAdapter.OnItemClickListener() {
                @Override
                public void onItemClick(View view, int position) {
                    // Get the selected item
                    MapFile selectedFile = mapFiles.get(position);
                    Log.d("SS1234", "Selected file: " + selectedFile.getMapFile());
                    Uri selectedMapUri = Uri.parse(path+selectedFile.getMapFile().toString());
                    Log.d("SS1234","selectedMapUri : " + selectedMapUri);

                    // Create an Intent to start MainActivity
                    Intent intent = new Intent(mapFileListActivity.this, MainActivity.class);
                    intent.putExtra("mapPath",selectedMapUri);
                    Singleton singleton = Singleton.getInstance();
                    singleton.setSelectedMapFile(selectedFile.getMapFile());

                    // Start MainActivity
                    startActivity(intent);
                }
            });
        }
    }

    private void loadImagesFromGallery() {
        // 여기에 이미지를 가져오는 코드를 추가합니다.
        ContentResolver contentResolver = getContentResolver();
        Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        Log.d("SS1234","444444444444444444");
        String[] projection = {
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATA
        };
        Log.d("SS1234","projection : " + projection.length);
        Cursor cursor = contentResolver.query(uri, projection, null, null, null);

        if (cursor != null) {
            while (cursor.moveToNext()) {
                int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
                int nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME);
                int dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);

                long id = cursor.getLong(idColumn);
                String name = cursor.getString(nameColumn);
                String data = cursor.getString(dataColumn);

                // 이 데이터를 사용하여 이미지 목록을 구성합니다.
                Log.d("SS1234", "GalleryImage    ID: " + id + ", Name: " + name + ", Path: " + data);


            }
            cursor.close();
        }
    }

}
