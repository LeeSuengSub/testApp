package com.example.testapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.text.InputFilter;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;
import com.example.testapp.domain.Icon;
import com.example.testapp.singleton.Singleton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity{

    private String path = "/storage/emulated/0/Android/data/com.example.testapp/files/";
    private static final String[] REQUIRED_PERMISSIONS = {Manifest.permission.READ_EXTERNAL_STORAGE};
    private static final int REQUEST_CODE_PERMISSIONS = 1001;
    private static final int REQUEST_IMAGE_PICK = 1;
    private static final int PICK_CSV_FILE_REQUEST = 1;
    private static final int REQUEST_PERMISSION = 100;
    private static final int REQUEST_STORAGE_PERMISSION = 1;
    private static final String TEST = "test";
    private ActivityResultLauncher<Intent> activityResultLauncher;
    private SubsamplingScaleImageView scaleView;
    private FloatingActionButton fab; //아이콘 배치 버튼
    private TextView nfcTitle;
    private TextView pathText;
    private ScaleGestureDetector scaleGestureDetector;
    private float scaleFactor = 1.0f;
    private PendingIntent pendingIntent;
    private NfcAdapter nfcAdapter; //NFC
    private ToggleButton toggleButton;
    private boolean isCheck = false; //설정 잠금.
    private boolean selfCheck = false; //수정
    private boolean isDelete = false; //삭제
    private Bitmap currentBitmap;
    private Paint paint;
    private HashSet<String> readNfcContents = new HashSet<>();
    private Icon closestIcon = null;
//    String selectedImagePath = null;
    private String mapImageName;
    List<Icon> icons = new ArrayList<>();
    //========
    private long backpressedTime = 0;
    //========

    private void checkAndRequestPermission(String permission, int requestCode) {
        if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{permission}, requestCode);
        } else {
            Log.i("MainActivity", permission + " permission has already been granted.");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


//        if (requestCode == REQUEST_IMAGE_PICK && resultCode == RESULT_OK && data != null) {
//            Uri selectedImageUri = data.getData();
//            if (selectedImageUri != null) {
//                String imagePath = getPathFromUri(selectedImageUri);
//                if (imagePath != null) {
//                    scaleView.setImage(ImageSource.uri(imagePath));
//                }
//            }
//        }

        if (requestCode == REQUEST_IMAGE_PICK && resultCode == RESULT_OK && data != null) {
            try {
                Uri selectedImageUri = data.getData();
                if (selectedImageUri != null) {
                    String imagePath = getPathFromUri(selectedImageUri);
                    if (imagePath != null) {
                        scaleView.setImage(ImageSource.uri(imagePath));
                    } else {
                        Toast.makeText(this, "이미지 경로를 얻을 수 없습니다.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, "이미지를 선택하지 않았습니다.", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "이미지 처리 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fab= findViewById(R.id.fab);
        nfcTitle = findViewById(R.id.nfcTitle);
        scaleView = findViewById(R.id.imageView);
        toggleButton = findViewById(R.id.toggleButton);
        pathText = findViewById(R.id.pathContext);

        //권한 확인.
//        checkAndRequestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
//        checkAndRequestPermission(Manifest.permission.READ_EXTERNAL_STORAGE, MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);

        // 외부 저장소 접근 권한 확인 및 요청
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            // 권한이 없는 경우 권한 요청
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_CODE_PERMISSIONS);
        }

        //android 버전에 따른 권한 이슈
        // Android 12L (API level 32) 이하에서는 READ_EXTERNAL_STORAGE 권한을 요청합니다.
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        REQUEST_STORAGE_PERMISSION);
            } else {
                // 권한이 이미 부여된 경우에 대한 처리를 여기에 추가합니다.
                // 예: 파일 읽기 작업 수행
                performReadOperation();
            }
        } else { // Android 13 (API level 33) 이상에서는 다른 권한을 요청합니다.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                requestMediaPermissions();
            }
        }

        if (allPermissionsGranted()) {
//            loadImagesFromGallery();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }

        // 이미지 선택 시 전달된 데이터 가져오기
        if (getIntent() != null && getIntent().hasExtra("selected_image_path")) {
            String selectedImagePath = getIntent().getStringExtra("selected_image_path");
            mapImageName = selectedImagePath;

            // Glide를 사용하여 이미지 로드 및 설정
            Glide.with(this)
                    .downloadOnly()
                    .load(selectedImagePath)
                    .into(new CustomTarget<File>() {
                        @Override
                        public void onResourceReady(@NonNull File resource, @Nullable Transition<? super File> transition) {
                            // 이미지 로드가 완료되면 SubsamplingScaleImageView에 이미지 설정
                            ImageSource imageSource = ImageSource.uri(Uri.fromFile(resource));
                            runOnUiThread(() -> scaleView.setImage(imageSource));
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {
                            // 이미지 로드가 취소될 때 처리할 내용
                        }
                    });
        }

        // 원본 이미지 로드
//        currentBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.test_jpg);
        // 원본 이미지를 화면에 표시합니다.
//        scaleView.setImage(ImageSource.bitmap(currentBitmap));
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);

        //지도 더블 탭 기능 막는 코드.
        scaleView.setDoubleTapZoomStyle(SubsamplingScaleImageView.ZOOM_FOCUS_CENTER);
        scaleView.setDoubleTapZoomDpi(0);

        //스마트폰에 NFC가 없거나 꺼져있는 경우
        if(nfcAdapter == null || !nfcAdapter.isEnabled()) {
            Toast.makeText(this, "NFC기능이 꺼져있습니다.", Toast.LENGTH_SHORT).show();
        }

        Intent intent = new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_MUTABLE);
        scaleGestureDetector = new ScaleGestureDetector(this, new ScaleListener());

        fab.setOnClickListener(view -> {
            isCheck = !isCheck;
            fab.setImageResource(isCheck ? android.R.drawable.ic_delete : android.R.drawable.ic_lock_lock);

            //isCheck에 따라 수정/삭제 기능 -> 활성화/비활성화 해야함.
            if(isDelete) {
                isDelete = false;
                nfcTitle.setText("====");
                nfcTitle.setTextColor(Color.BLACK);
                nfcTitle.setTypeface(Typeface.DEFAULT);
            }

            if(selfCheck) {
                selfCheck = false;
                toggleButton.setVisibility(View.INVISIBLE);
            }
        });

        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);

        // CSV 파일이 있다면, CSV 파일을 읽어서 아이콘을 그립니다.
//        if (csvFile.exists()) {
//            readCsvAndDrawIcons();
//            drawIcons();
//        }
//        selectCsvFile();
        scaleView.setOnTouchListener((view, motionEvent) -> {

            PointF viewCoord = new PointF(motionEvent.getX(), motionEvent.getY());
            PointF imageCoord = scaleView.viewToSourceCoord(viewCoord);

            if (isCheck && motionEvent.getAction() == MotionEvent.ACTION_UP) {
                // NFC 태그의 내용을 가져옵니다.
                String nfcText = nfcTitle.getText().toString();

                if(Singleton.getInstance().getSelectedMapFile() == null) {
                    Toast.makeText(this, "도면을 선택해주세요.", Toast.LENGTH_SHORT).show();
                    return false;
                }

                //테스트를 위한 코드
                if(nfcText.equals("====")) {
                    nfcText = TEST;
                }

                //NFC 태그의 내용이 없는 경우
                if(nfcText.isEmpty() || nfcText.equals("====")) {
                    Toast.makeText(this, "NFC 태그를 인식해주세요.", Toast.LENGTH_SHORT).show();
                    return false;
                }


                // CSV 파일을 읽고 화면에 아이콘을 표시합니다.
//                readCsvAndDrawIcons();
                icons.add(new Icon(imageCoord, nfcText));
                drawIcons();
                saveIconsToCsv();

                return false;
            }

            //아이콘의 좌표이동
            if(selfCheck && toggleButton.getText().equals("좌표") && motionEvent.getAction() == MotionEvent.ACTION_MOVE) {
                // 터치 지점에서 가장 가까운 아이콘을 찾습니다.
                float closestDistance = Float.MAX_VALUE;

                for (Icon icon : icons) {
                    float distance = (float) Math.hypot(icon.point.x - imageCoord.x, icon.point.y - imageCoord.y);
                    if (distance < closestDistance) {
                        closestIcon = icon;
                        closestDistance = distance;
                    }
                }

                // 가장 가까운 아이콘이 선택 영역 내에 있다면, 그 아이콘을 손가락의 움직임에 따라 움직입니다.
                if (closestDistance < 100) {
                    PointF newImageCoord = scaleView.viewToSourceCoord(new PointF(motionEvent.getX(), motionEvent.getY()));

                    // 손가락이 움직인 거리를 계산합니다.
                    float dx = newImageCoord.x - closestIcon.point.x;
                    float dy = newImageCoord.y - closestIcon.point.y;

                    // 아이콘의 새로운 위치를 결정합니다.
                    closestIcon.point.x += dx;
                    closestIcon.point.y += dy;

                    drawIcons();
                }

                return true;

            //아이콘의 텍스트 수정
            }else if (toggleButton.getText().equals("텍스트") && toggleButton.isShown() && motionEvent.getAction() == MotionEvent.ACTION_UP) {
                // 터치 지점에서 가장 가까운 아이콘을 찾습니다.
                float closestDistance = Float.MAX_VALUE;

                for (Icon icon : icons) {
                    float distance = (float) Math.hypot(icon.point.x - imageCoord.x, icon.point.y - imageCoord.y);
                    if (distance < closestDistance) {
                        closestIcon = icon;
                        closestDistance = distance;
                    }
                }

                // 가장 가까운 아이콘이 선택 영역 내에 있다면, 그 아이콘을 손가락의 움직임에 따라 움직입니다.
                if (closestDistance < 150) {
                    // 수정할 내용을 적을 EditText
                    EditText editText = new EditText(MainActivity.this);
                    // 수정할 내용의 길이 제한 5자릿수.(예: 12345)
                    InputFilter[] filters = new InputFilter[1];
                    filters[0] = new InputFilter.LengthFilter(5);
                    editText.setFilters(filters);
                    // dialog를 생성하여 수정할 내용을 입력할 공간 확보.
                    AlertDialog dialog = new AlertDialog.Builder(MainActivity.this)
                            .setTitle(closestIcon.text+" 수정")
                            .setMessage("수정할 내용을 입력해주세요.")
                            .setView(editText)
                            .setPositiveButton("OK", null)
                            .setNegativeButton("Cancel", null)
                            .create();

                    dialog.setOnShowListener(new DialogInterface.OnShowListener() {
                        @Override
                        public void onShow(DialogInterface dialogInterface) {
                            Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                            button.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    if (editText.getText().toString().isEmpty()) {
                                        // EditText가 비어있다면, dialog를 닫지 않고 Toast 메시지를 띄웁니다.
                                        Toast.makeText(MainActivity.this, "수정할 내용을 입력해주세요", Toast.LENGTH_SHORT).show();
                                    } else {
                                        // EditText가 비어있지 않다면 AlertDialog를 닫습니다.
                                        closestIcon.text = editText.getText().toString();
                                        dialog.dismiss();
                                    }
                                }
                            });
                        }
                    });

                    dialog.show();

                }

                return false;
            }

            //아이콘 삭제.
            if (!isCheck && nfcTitle.getText().toString().equals("삭제") && motionEvent.getAction() == MotionEvent.ACTION_UP) {
                // 터치 지점에 가장 가까운 아이콘을 찾습니다.

                float closestDistance = Float.MAX_VALUE;

                for (Icon icon : icons) {
                    float distance = (float) Math.hypot(icon.point.x - imageCoord.x, icon.point.y - imageCoord.y);
                    if (distance < closestDistance) {
                        closestIcon = icon;
                        closestDistance = distance;
                    }
                }

                // 가장 가까운 아이콘이 선택 영역 내에 있다면, 그 아이콘의 텍스트를 수정합니다.
                if (closestDistance < 100) {
                    // 조건이 충족되면 다이얼로그를 띄웁니다.
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("삭제")
                            .setMessage(closestIcon.text + "를 삭제하시겠습니까?")
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    icons.remove(closestIcon);
                                    readNfcContents.remove(closestIcon.text);
                                    drawIcons();

                                    // CSV 파일을 다시 씁니다.
                                    try {
//                                        FileWriter writer = new FileWriter("nfcLight.csv", false); // false for overwrite mode
                                        FileWriter writer = new FileWriter(Singleton.getInstance().getSelectedCsvFile(), false); // false for overwrite mode
                                        for (Icon icon : icons) {
                                            writer.append(icon.text + "," + Math.round(icon.point.x) + "," + Math.round(icon.point.y) + "\n");
                                        }
                                        writer.flush();
                                        writer.close();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                }
                return false;
            }

            return false; // onTouch 이벤트를 여기서 끝내지 않고, 다음 이벤트로 넘깁니다.
        });

    }//onCreate

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }


    private String getPathFromUri(Uri uri) {
        String[] projection = { MediaStore.Images.Media.DATA };
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
        if (cursor != null) {
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            String path = cursor.getString(column_index);
            cursor.close();
            return path;
        }
        return null;
    }

    // Android 13 (API level 33) 이상에서 사용될 권한 요청 메서드
    @RequiresApi(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private void requestMediaPermissions() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this,
                        Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_MEDIA_IMAGES,
                            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED},
                    REQUEST_STORAGE_PERMISSION);
        } else {
            // 권한이 이미 부여된 경우에 대한 처리를 여기에 추가합니다.
            // 예: 미디어 파일 읽기 작업 수행
            performMediaReadOperation();
        }
    }

    // 권한 요청 결과 처리
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                loadImagesFromGallery();
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_MEDIA_IMAGES},
                        REQUEST_CODE_PERMISSIONS);
            }
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
//                Log.d("SS1234", "GalleryImage    ID: " + id + ", Name: " + name + ", Path: " + data);


            }
            cursor.close();
        }
    }

//    private void selectCsvFile() {
//        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
//        intent.setType("text/csv");
//        intent.addCategory(Intent.CATEGORY_OPENABLE);
//        startActivityForResult(Intent.createChooser(intent, "Select CSV File"), PICK_CSV_FILE_REQUEST);
//    }


    // 파일 읽기 작업을 수행하는 메서드 예시
    private void performReadOperation() {
        // 파일 읽기 작업을 수행하는 코드를 여기에 추가합니다.
        // 예: 외부 저장소에서 파일을 읽어오는 등의 작업
        Toast.makeText(this, "1111111111111", Toast.LENGTH_SHORT).show();
    }

    // 미디어 파일 읽기 작업을 수행하는 메서드 예시
    private void performMediaReadOperation() {
        // 미디어 파일 읽기 작업을 수행하는 코드를 여기에 추가합니다.
        // 예: 이미지, 비디오 등을 읽어오는 작업
        Toast.makeText(this, "222222222222222", Toast.LENGTH_SHORT).show();
    }


    // 앱 전용 디렉토리에서 파일 읽기
//    public String readFileFromAppSpecificDirectory(Context context) {
//        String filename = "example.txt";
//        FileInputStream fis;
//        StringBuilder stringBuilder = new StringBuilder();
//        try {
//            fis = context.openFileInput(filename);
//            InputStreamReader inputStreamReader = new InputStreamReader(fis);
//            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
//            String line;
//            while ((line = bufferedReader.readLine()) != null) {
//                stringBuilder.append(line);
//            }
//            fis.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return stringBuilder.toString();
//    }


    private void saveIconsToCsv() {

        try {
            String filename = getCurrentTimeStamp() + ".csv";
            File file = new File(getFilesDir(), filename);
            String path = file.getAbsolutePath();
            FileOutputStream fos = openFileOutput(filename, MODE_APPEND);
            OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);

            for (Icon icon : icons) {
                String text = icon.text;
                float x = icon.point.x;
                float y = icon.point.y;
                String line = text + "," + x + "," + y + "\n";
                osw.write(line);
            }

            osw.flush();
            osw.close();
            fos.close();
            Log.d("FileWrite", "내용이 성공적으로 추가되었습니다.");
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "CSV 파일을 저장하는 중에 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    private void readIconsFromCsv() {
        try {
            FileInputStream fis = openFileInput(Singleton.getInstance().getSelectedCsvFile());
            InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
            BufferedReader reader = new BufferedReader(isr);
            String line;

            icons.clear(); // 아이콘 리스트 초기화

            Log.d("FileRead", "내용을 성공적으로 읽었습니다.");

            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 3) {
                    String text = parts[0];
                    float x = Float.parseFloat(parts[1]);
                    float y = Float.parseFloat(parts[2]);
                    Log.d("FileRead", "text: " + text + ", x: " + x + ", y: " + y);
                    icons.add(new Icon(new PointF(x, y), text));
                }
            }

//            drawIcons();

            reader.close();
            isr.close();
            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "CSV 파일을 읽는 중에 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        private static final float MIN_SCALE_FACTOR = 1.0f; // 최소 스케일링 팩터
        private static final float MAX_SCALE_FACTOR = 5.0f; // 최대 스케일링 팩터

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            scaleFactor *= detector.getScaleFactor();
            scaleFactor = Math.max(MIN_SCALE_FACTOR, Math.min(scaleFactor, MAX_SCALE_FACTOR));

            // 스케일링 팩터를 조절하여 이미지 뷰에 적용
            scaleView.setScaleX(scaleFactor);
            scaleView.setScaleY(scaleFactor);

            return true;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (nfcAdapter != null) {
            nfcAdapter.enableForegroundDispatch(this, pendingIntent, null, null);
        }

        Singleton singleton = Singleton.getInstance();
        String selectedCsvFileName = singleton.getSelectedCsvFile();
        String selectedMapFileName = singleton.getSelectedMapFile();

        if(selectedCsvFileName != null) {
            readIconsFromCsv();
            drawIcons();
        }

        pathText.setText(selectedCsvFileName + " / " + selectedMapFileName);
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (nfcAdapter != null) {
            nfcAdapter.disableForegroundDispatch(this);
        }
        finish();
    }

    private String readNfcTag(Intent intent) {
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
            Ndef ndef = Ndef.get((Tag)intent.getParcelableExtra(NfcAdapter.EXTRA_TAG));
            if (ndef != null) {
                try {
                    ndef.connect();
                    NdefMessage ndefMessage = ndef.getNdefMessage();
                    NdefRecord[] records = ndefMessage.getRecords();
                    if (records != null && records.length > 0) {
                        NdefRecord record = records[0];
//                        String payload = new String(record.getPayload(), StandardCharsets.UTF_8);
                        String payload = new String(Arrays.copyOfRange(record.getPayload(), 3, record.getPayload().length), StandardCharsets.UTF_8);
                        return payload;
                    }
                } catch (IOException | FormatException e) {
                    e.printStackTrace();
                } catch (NullPointerException e){
                    e.printStackTrace();
                    Toast.makeText(this, "다시 태그해주세요.", Toast.LENGTH_SHORT).show();
                } finally {
                    try {
                        ndef.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return null;
    }

    //지도 파일 선택창
    public void onClickLandscapeOption1(View view) {
        Intent intent = new Intent(MainActivity.this, imageActivity.class);
        startActivity(intent);
    }

    //CSV 파일 선택창
    public void onClickLandscapeOption2(View view) {
        Intent intent = new Intent(MainActivity.this, CsvFileListActivity.class);
        startActivity(intent);
    }

    //수정 버튼.
    public void onClickLandscapeOption3(View view) {
        selfCheck = selfCheck ? false : true;
        toggleButton.setVisibility(selfCheck ? View.VISIBLE : View.INVISIBLE);

        //삭제에서 바로 수정으로 넘어갈 때.
        if(isDelete) {
            isDelete = false;
            nfcTitle.setText("====");
            nfcTitle.setTextColor(Color.BLACK);
            nfcTitle.setTypeface(Typeface.DEFAULT);
        }
    }

    //삭제 버튼.
    public void onClickLandscapeOption4(View view) {
        isDelete = isDelete ? false : true;
        nfcTitle.setText(isDelete ? "삭제" : "====");
        nfcTitle.setTextColor(isDelete ? Color.RED : Color.BLACK);
        nfcTitle.setTypeface(isDelete ? Typeface.defaultFromStyle(Typeface.BOLD) : Typeface.DEFAULT);

        //수정에서 바로 삭제로 넘어갈 때.
        if(selfCheck) {
            selfCheck = false;
            toggleButton.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        setIntent(intent);

        String nfcText = readNfcTag(intent);

        // NFC 태그의 내용이 이미 읽힌 내용인지 확인합니다.
        if (readNfcContents.contains(nfcText)) {
            Toast.makeText(this, "이미 읽은 NFC 태그입니다.", Toast.LENGTH_SHORT).show();
            nfcTitle.setText("====");
            return;
        }

        // NFC 태그의 내용을 가져오지 못한 경우
        if (nfcText == null) {
            Toast.makeText(this, "NFC 태그를 읽는데 실패했습니다.", Toast.LENGTH_SHORT).show();
            nfcTitle.setText(nfcText);
            return;
        }

        // 테스트를 위한 부분.
        // 테스트가 끝나면 지워야 함.
        if(nfcTitle.equals("====")) {
            nfcTitle.setText(TEST);
            return;
        }

        // 터치 이벤트가 끝났을 때
        PointF viewCoord = new PointF(scaleView.getWidth() / 2, scaleView.getHeight() / 2);
        PointF imageCoord = scaleView.viewToSourceCoord(viewCoord);

        // 좌표와 NFC의 내용을 CSV파일에 저장
        try {
            File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
//            File csvFile = new File(dir, "nfcLight.csv");
            File csvFile = new File(dir, Singleton.getInstance().getSelectedCsvFile());
            FileWriter writer = new FileWriter(csvFile, true); // true for append mode
            writer.append(nfcText + "," + imageCoord.x + "," + imageCoord.y + "\n");
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }//onNewIntent

    private void drawIcons() {
        // Paint 객체를 초기화합니다.
        if (paint == null) {
            // 빨간 동그라미 Paint 객체를 생성합니다.
            paint = new Paint();
            paint.setColor(Color.RED);
            paint.setStyle(Paint.Style.FILL);
        }

        // 텍스트를 그리기 위한 Paint 객체를 생성합니다.
        Paint textPaint = new Paint();
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(50);
        textPaint.setStyle(Paint.Style.FILL);

        // 현재의 확대/축소 수준과 중심 좌표를 저장합니다.
        float currentScale = scaleView.getScale();
        PointF currentCenter = scaleView.getCenter();

        String selectedImagePath = getIntent().getStringExtra("selected_image_path");

        Glide.with(this)
                .downloadOnly()
                .load(selectedImagePath)
                .into(new CustomTarget<File>() {
                    @Override
                    public void onResourceReady(@NonNull File resource, @Nullable Transition<? super File> transition) {
                        // 이미지 로드가 완료되면 BitmapFactory를 사용하여 Bitmap을 만듭니다.
                        Bitmap originalBitmap = BitmapFactory.decodeFile(resource.getAbsolutePath());
                        Bitmap mutableBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);

                        // 캔버스를 생성하고 원본 이미지를 그립니다.
                        Canvas canvas = new Canvas(mutableBitmap);

                        for (Icon icon : icons) {
                            // 좌표에 원을 그립니다.
                            canvas.drawCircle(icon.point.x, icon.point.y, 30, paint);

                            // 좌표에 텍스트를 그립니다.
                            canvas.drawText(icon.text, icon.point.x - 40, icon.point.y + 15, textPaint);
                        }

                        // 수정된 이미지를 화면에 표시합니다.
                        runOnUiThread(() -> {
                            scaleView.setImage(ImageSource.bitmap(mutableBitmap));

                            // 저장한 확대/축소 수준과 중심 좌표를 복원합니다.
                            scaleView.setScaleAndCenter(currentScale, currentCenter);
                        });

                        // 현재 이미지를 저장합니다.
                        currentBitmap = mutableBitmap;
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                        // 이미지 로드가 취소될 때 처리할 내용
                    }
                });
    }//drawIcons

    private String getCurrentTimeStamp() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        return dateFormat.format(new Date());
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Toast.makeText(this, "어플이 종료됩니다.", Toast.LENGTH_SHORT).show();
        finishAffinity();
        System.runFinalization();
        System.exit(0);
    }

}// MainActivity.java