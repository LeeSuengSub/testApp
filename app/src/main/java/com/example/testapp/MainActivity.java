package com.example.testapp;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Typeface;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import android.os.Environment;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.appcompat.app.AppCompatActivity;

import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;
import com.example.testapp.domain.Icon;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class MainActivity extends AppCompatActivity{

    private static final String TEST = "test";
    private SubsamplingScaleImageView scaleView;
    private FloatingActionButton fab;
    private TextView nfcTitle;
    private ScaleGestureDetector scaleGestureDetector;
    private float scaleFactor = 1.0f;
    private PendingIntent pendingIntent;
    private boolean isCheck = false; //설정 잠금.
    private NfcAdapter nfcAdapter; //NFC
    private ToggleButton toggleButton;
    private boolean selfCheck = false;
    private boolean isDelete = false;
    private Bitmap currentBitmap;
    private Paint paint;
    private HashSet<String> readNfcContents = new HashSet<>();

    //========
    List<Icon> icons = new ArrayList<>();
    File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
    File csvFile = new File(dir, "nfcLight.csv");
    //========

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fab= findViewById(R.id.fab);
        nfcTitle = findViewById(R.id.nfcTitle);
        scaleView = findViewById(R.id.imageView);
        toggleButton = findViewById(R.id.toggleButton);

        // 원본 이미지 로드
        currentBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.test);
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);

        // 원본 이미지를 화면에 표시합니다.
        scaleView.setImage(ImageSource.bitmap(currentBitmap));

        if(nfcAdapter == null || !nfcAdapter.isEnabled()) {
            Toast.makeText(this, "NFC기능이 꺼져있습니다.", Toast.LENGTH_SHORT).show();
        }

        Intent intent = new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_MUTABLE);
        scaleGestureDetector = new ScaleGestureDetector(this, new ScaleListener());

        fab.setOnClickListener(view -> {
            isCheck = !isCheck;
            fab.setImageResource(isCheck ? android.R.drawable.ic_delete : android.R.drawable.ic_lock_lock);
        });

        // CSV 파일이 있다면, CSV 파일을 읽어서 아이콘을 그립니다.
        if (csvFile.exists()) {
            readCsvAndDrawIcons();
            drawIcons();
        }

        scaleView.setOnTouchListener((view, motionEvent) -> {

            PointF viewCoord = new PointF(motionEvent.getX(), motionEvent.getY());
            PointF imageCoord = scaleView.viewToSourceCoord(viewCoord);


            if (isCheck && motionEvent.getAction() == MotionEvent.ACTION_UP) {
                // NFC 태그의 내용을 가져옵니다.
                String nfcText = nfcTitle.getText().toString();

                //테스트를 위한 코드
                if(nfcText.equals("====")) {
                    nfcText = TEST;
                }

                //NFC 태그의 내용이 없는 경우
                if(nfcText.isEmpty() || nfcText.equals("====")) {
                    Toast.makeText(this, "NFC 태그를 인식해주세요.", Toast.LENGTH_SHORT).show();
                    return false;
                }

                // 좌표와 NFC 태그의 내용을 CSV파일에 저장
                try {
                    File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                    File csvFile = new File(dir, "nfcLight.csv");
                    FileWriter writer = new FileWriter(csvFile, true); // true for append mode
                    writer.append(nfcText + "," + imageCoord.x + "," + imageCoord.y + "\n");
                    writer.flush();
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // CSV 파일을 읽고 화면에 아이콘을 표시합니다.
                readCsvAndDrawIcons();
                drawIcons();

                return false;
            }

            //아이콘의 좌표이동
            if(selfCheck && toggleButton.getText().equals("좌표") && motionEvent.getAction() == MotionEvent.ACTION_MOVE) {
                // 터치 지점에서 가장 가까운 아이콘을 찾습니다.
                Icon closestIcon = null;
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
                    PointF newImageCoord = scaleView.viewToSourceCoord(new PointF(motionEvent.getX(), motionEvent.getY()));

                    // Calculate the distance between the current position and the new position
                    float dx = newImageCoord.x - closestIcon.point.x;
                    float dy = newImageCoord.y - closestIcon.point.y;
                    float distance = (float) Math.hypot(dx, dy);

                    // Maximum allowed distance
                    float maxDistance = 100; // Change this to the value you want

                    // If the distance is greater than the maximum allowed distance, limit the movement
                    if (distance > maxDistance) {
                        // Calculate the direction from the current position to the new position
                        float directionX = dx / distance;
                        float directionY = dy / distance;

                        // Set the new position to be the current position moved in the calculated direction by the maximum allowed distance
                        newImageCoord.x = closestIcon.point.x + directionX * maxDistance;
                        newImageCoord.y = closestIcon.point.y + directionY * maxDistance;
                    }

                    closestIcon.point = newImageCoord;
                    drawIcons();
                }

                return false;

            //아이콘의 텍스트 수정
            }else if (toggleButton.getText().equals("텍스트") && toggleButton.isShown() && motionEvent.getAction() == MotionEvent.ACTION_UP) {
                // 터치 지점에서 가장 가까운 아이콘을 찾습니다.
                Icon closestIcon = null;
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
                    closestIcon.point = scaleView.viewToSourceCoord(new PointF(motionEvent.getX(), motionEvent.getY()));
                    drawIcons();
                }

                return false;
            }

            //아이콘 삭제.
            if (!isCheck && nfcTitle.getText().toString().equals("삭제") && motionEvent.getAction() == MotionEvent.ACTION_UP) {
                // 터치 지점에 가장 가까운 아이콘을 찾습니다.
                Icon closestIcon = null;
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
//                    closestIcon.text = "새로운 텍스트"; // 여기에 원하는 텍스트를 입력하세요.
//                    drawIcons();

                    // 조건이 충족되면 다이얼로그를 띄웁니다.
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("삭제")
                            .setMessage(closestIcon.text + "를 삭제하시겠습니까?")
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    //삭제 작업을 진행해야 함.
                                    Toast.makeText(MainActivity.this, "삭제", Toast.LENGTH_SHORT).show();
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
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (nfcAdapter != null) {
            nfcAdapter.disableForegroundDispatch(this);
        }
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
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        String nfcText = readNfcTag(intent);

        // NFC 태그의 내용이 이미 읽힌 내용인지 확인합니다.
        if (readNfcContents.contains(nfcText)) {
            Toast.makeText(this, "이미 읽은 NFC 태그입니다.", Toast.LENGTH_SHORT).show();
            nfcTitle.setText("====");
            return;
        }

        if(readNfcContents.contains("====")) {
            Toast.makeText(this, "NFC를 태그해주세요.", Toast.LENGTH_SHORT).show();
            nfcTitle.setText(TEST);
            return;
        }

        nfcTitle.setText(nfcText);

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
            File csvFile = new File(dir, "nfcLight.csv");
            FileWriter writer = new FileWriter(csvFile, true); // true for append mode
            writer.append(nfcText + "," + imageCoord.x + "," + imageCoord.y + "\n");
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void readCsvAndDrawIcons() {
        File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File csvFile = new File(dir, "nfcLight.csv");

        try {
            FileReader fileReader = new FileReader(csvFile);
            BufferedReader bufferedReader = new BufferedReader(fileReader);

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                String[] fields = line.split(",");
                if (fields.length == 3) {
                    String text = fields[0];
                    float x = Float.parseFloat(fields[1]);
                    float y = Float.parseFloat(fields[2]);
                    PointF point = new PointF(x, y);

                    // 아이콘 객체를 생성하고 리스트에 추가합니다.
                    icons.add(new Icon(point, text));

                    // NFC 태그의 내용을 HashSet에 추가합니다.
                    readNfcContents.add(text);
                }
            }

            bufferedReader.close();
            fileReader.close();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "CSV파일을 읽는 중에 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
        }
    }

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
//        Bitmap mutableBitmap = currentBitmap.copy(Bitmap.Config.ARGB_8888, true);
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

}// MainActivity.java