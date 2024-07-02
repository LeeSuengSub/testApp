package com.example.testapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Typeface;
import android.net.Uri;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import android.os.Environment;
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

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

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

    private static final String path = "/storage/emulated/0/Android/data/com.example.testapp/files/csv";
    private static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1;
    private static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE =2;
    private static final int REQUEST_CODE_OPEN_DOCUMENT = 42;
    private static final String TEST = "test";
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
    //========
    List<Icon> icons = new ArrayList<>();
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
        if (requestCode == 1 && resultCode == RESULT_OK) {
            String selectedMapFileName = data.getStringExtra("selectedMapFile");
            File mapFileDirectory = new File(getFilesDir(), "map");
            File mapFile = new File(mapFileDirectory, selectedMapFileName);

            if (mapFile.exists()) {
                scaleView.setImage(ImageSource.uri(Uri.fromFile(mapFile)));
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
        checkAndRequestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
        checkAndRequestPermission(Manifest.permission.READ_EXTERNAL_STORAGE, MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);

        // 원본 이미지 로드
//        currentBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.test_jpg);
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);

        // 원본 이미지를 화면에 표시합니다.
//        scaleView.setImage(ImageSource.bitmap(currentBitmap));

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

        // CSV 파일이 있다면, CSV 파일을 읽어서 아이콘을 그립니다.
//        if (csvFile.exists()) {
//            readCsvAndDrawIcons();
//            drawIcons();
//        }

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

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.i("MainActivity", "Write External Storage permission has been granted.");
                } else {
                    Log.i("MainActivity", "Write External Storage permission was denied.");
                }
                break;
            }
            case REQUEST_CODE_OPEN_DOCUMENT: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.i("MainActivity", "REQUEST_CODE_OPEN_DOCUMENT permission has been granted.");
                } else {
                    Log.i("MainActivity", "REQUEST_CODE_OPEN_DOCUMENT permission was denied.");
                }
                break;
            }
        }
    }

    private void saveIconsToCsv() {

        try {

            String filename = getCurrentTimeStamp() + ".csv";
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
        Intent intent = new Intent(MainActivity.this, mapFileListActivity.class);
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

    private void drawCircleOnImage(PointF point, String text) {
        // Paint 객체를 초기화합니다.
        if (paint == null) {
            paint = new Paint();
            paint.setColor(Color.RED);
            paint.setStyle(Paint.Style.FILL);
        }

        Paint textPaint = new Paint();
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(30);
        textPaint.setStyle(Paint.Style.FILL);

        // 현재의 확대/축소 수준과 중심 좌표를 저장합니다.
        float currentScale = scaleView.getScale();
        PointF currentCenter = scaleView.getCenter();

        // 원본 이미지를 수정 가능한 복사본으로 만듭니다.
        Bitmap mutableBitmap = currentBitmap.copy(Bitmap.Config.ARGB_8888, true);

        // 캔버스를 생성하고 원본 이미지를 그립니다.
        Canvas canvas = new Canvas(mutableBitmap);

        // 좌표에 원을 그립니다.
        canvas.drawCircle(point.x, point.y, 30, paint);

        // 좌표에 텍스트를 그립니다.
        canvas.drawText(text, point.x, point.y, textPaint);

        // 수정된 이미지를 화면에 표시합니다.
        runOnUiThread(() -> {
            scaleView.setImage(ImageSource.bitmap(mutableBitmap));

            // 저장한 확대/축소 수준과 중심 좌표를 복원합니다.
            scaleView.setScaleAndCenter(currentScale, currentCenter);
        });

        // 현재 이미지를 저장합니다.
        currentBitmap = mutableBitmap;
    }//drawCircleOnImage

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

        // 원본 이미지를 수정 가능한 복사본으로 만듭니다.
        Bitmap originalBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.test_jpg);
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
    }//drawIcons

    private String getCurrentTimeStamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        return sdf.format(new Date());
    }

}// MainActivity.java