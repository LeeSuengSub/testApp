package com.example.testapp;

import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
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
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity{

    private SubsamplingScaleImageView scaleView;
    private FloatingActionButton fab;
    private FrameLayout frameLayout;
    private TextView nfcTitle;
    private Bitmap iconBitmap;
    private ImageView imageView;
    private ScaleGestureDetector scaleGestureDetector;
    private float scaleFactor = 1.0f;
    private PendingIntent pendingIntent;
    private boolean isCheck = false; //설정 잠금.
    private NfcAdapter nfcAdapter; //NFC

    private Bitmap currentBitmap;
    private Paint paint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fab= findViewById(R.id.fab);
        frameLayout = findViewById(R.id.frameLayout);
        nfcTitle = findViewById(R.id.nfcTitle);
//        imageView = findViewById(R.id.imageView);
        scaleView = findViewById(R.id.imageView);

        // 원본 이미지 로드
        currentBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.test);

        // 원본 이미지를 화면에 표시합니다.
        scaleView.setImage(ImageSource.bitmap(currentBitmap));

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);

        if(nfcAdapter == null || !nfcAdapter.isEnabled()) {
            Toast.makeText(this, "NFC기능이 꺼져있습니다.", Toast.LENGTH_SHORT).show();
        }

        Intent intent = new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_MUTABLE);

        scaleGestureDetector = new ScaleGestureDetector(this, new ScaleListener());
        //아이콘
        iconBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.icon);

        //아이콘 배치
        ImageView iconView = new ImageView(this);
        iconView.setImageBitmap(iconBitmap);

        fab.setOnClickListener(view -> {
            isCheck = !isCheck;
            fab.setImageResource(isCheck ? android.R.drawable.ic_delete : android.R.drawable.ic_lock_lock);
        });

//        scaleView.setOnTouchListener((view, motionEvent) -> {
//            if (isCheck && motionEvent.getAction() == MotionEvent.ACTION_UP) {
//                // 터치 이벤트가 끝났을 때
//                PointF viewCoord = new PointF(motionEvent.getX(), motionEvent.getY());
//                PointF imageCoord = scaleView.viewToSourceCoord(viewCoord);
//
//                // 현재의 확대/축소 수준과 중심 좌표를 저장합니다.
//                float currentScale = scaleView.getScale();
//                PointF currentCenter = scaleView.getCenter();
//
//                // 확대/축소를 비활성화합니다.
//                scaleView.setZoomEnabled(false);
//
//                // 중심 좌표를 다시 설정합니다.
//                scaleView.setScaleAndCenter(currentScale, currentCenter);
//
//                // 아이콘 그리기 동작을 수행
//                drawIconOnImage(imageCoord); // 화면 좌표를 사용하여 아이콘 그리기
//            }
//            return false; // onTouch 이벤트를 여기서 끝내지 않고, 다음 이벤트로 넘깁니다.
//        });
        readCsvAndDrawIcons();

        scaleView.setOnTouchListener((view, motionEvent) -> {
            if (isCheck && motionEvent.getAction() == MotionEvent.ACTION_UP) {
                // 터치 이벤트가 끝났을 때
                PointF viewCoord = new PointF(motionEvent.getX(), motionEvent.getY());
                PointF imageCoord = scaleView.viewToSourceCoord(viewCoord);

                // 아이콘 그리기 동작을 수행
//                drawIconOnImage(imageCoord); // 화면 좌표를 사용하여 아이콘 그리기
                drawCircleOnImage(imageCoord); // 화면 좌표를 사용하여 원 그리기
                // 좌표를 CSV파일에 저장
                try {
                    File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                    File csvFile = new File(dir, "coordinates.csv");
                    FileWriter writer = new FileWriter(csvFile, true); // true for append mode
                    writer.append(imageCoord.x + "," + imageCoord.y + "\n");
                    writer.flush();
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
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

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        String nfcText = readNfcTag(intent);

        nfcTitle.setText(nfcText);
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


    public void onClickLandscapeOption1(View view) {
        Intent intent = new Intent(MainActivity.this, mapFileListActivity.class);
        startActivity(intent);
    }

    public void onClickLandscapeOption2(View view) {
        Intent intent = new Intent(MainActivity.this, CsvFileListActivity.class);
        startActivity(intent);
    }

    public void onClickLandscapeOption3(View view) {

    }

    //이미지에 아이콘이 고정이 되지만 아이콘이 생성이 되지만 하나의 아이콘만 화면에 표시됨.
    /*
    private void drawIconOnImage(PointF point) {
        // 아이콘 이미지 로드
        Bitmap iconBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.icon);

        // iconBitmap이 null인지 확인
        if (iconBitmap == null) {
            // iconBitmap 초기화에 실패했을 때의 처리
            Toast.makeText(this, "아이콘 이미지 로드에 실패했습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 아이콘 이미지 크기 조정
        int iconWidth = 80; // 아이콘의 너비
        int iconHeight = 80; // 아이콘의 높이
        iconBitmap = Bitmap.createScaledBitmap(iconBitmap, iconWidth, iconHeight, false);

        // 현재의 확대/축소 수준과 중심 좌표를 저장합니다.
        float currentScale = scaleView.getScale();
        PointF currentCenter = scaleView.getCenter();

        // 원본 이미지 로드
        Bitmap finalIconBitmap = iconBitmap;
        Glide.with(this)
                .asBitmap()
                .load(R.drawable.test)
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        // 원본 이미지를 수정 가능한 복사본으로 만듭니다.
                        Bitmap mutableBitmap = resource.copy(Bitmap.Config.ARGB_8888, true);

                        // 캔버스를 생성하고 원본 이미지를 그립니다.
                        Canvas canvas = new Canvas(mutableBitmap);

                        // 아이콘을 이미지의 좌표에 그립니다.
                        canvas.drawBitmap(finalIconBitmap, point.x, point.y, null);

                        // 수정된 이미지를 화면에 표시합니다.
                        scaleView.setImage(ImageSource.bitmap(mutableBitmap));

                        // 저장한 확대/축소 수준과 중심 좌표를 복원합니다.
                        scaleView.setScaleAndCenter(currentScale, currentCenter);
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                    }
                });
    }
     */
    private void drawCircleOnImage(PointF point) {
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
        canvas.drawText("Here", point.x, point.y, textPaint);

        // 수정된 이미지를 화면에 표시합니다.
        runOnUiThread(() -> {
            scaleView.setImage(ImageSource.bitmap(mutableBitmap));

            // 저장한 확대/축소 수준과 중심 좌표를 복원합니다.
            scaleView.setScaleAndCenter(currentScale, currentCenter);
        });

        // 현재 이미지를 저장합니다.
        currentBitmap = mutableBitmap;
    }


//    private void drawIconOnImage(PointF point) {
//        // iconBitmap이 null인지 확인
//        if (iconBitmap == null) {
//            // iconBitmap 초기화에 실패했을 때의 처리
//            Toast.makeText(this, "아이콘 이미지 로드에 실패했습니다.", Toast.LENGTH_SHORT).show();
//            return;
//        }
//
//        // 아이콘 이미지 크기 조정
//        int iconWidth = 80; // 아이콘의 너비
//        int iconHeight = 80; // 아이콘의 높이
//        Bitmap scaledIconBitmap = Bitmap.createScaledBitmap(iconBitmap, iconWidth, iconHeight, false);
//
//        // 현재의 확대/축소 수준과 중심 좌표를 저장합니다.
//        float currentScale = scaleView.getScale();
//        PointF currentCenter = scaleView.getCenter();
//
//        // 원본 이미지를 수정 가능한 복사본으로 만듭니다.
//        if (mutableBitmap != null) {
//            currentBitmap = mutableBitmap.copy(Bitmap.Config.ARGB_8888, true);// mutableBitmap을 currentBitmap에 복사
//            mutableBitmap.recycle();//메모리 해제
//            canvas = new Canvas(currentBitmap);
//        }
//        mutableBitmap = currentBitmap.copy(Bitmap.Config.ARGB_8888, true);
//
//        // 캔버스를 생성하고 원본 이미지를 그립니다.
//        Canvas canvas = new Canvas(mutableBitmap);
//
//        // 아이콘을 이미지의 좌표에 그립니다.
//        canvas.drawBitmap(scaledIconBitmap, point.x, point.y, null);
//
//        // 수정된 이미지를 화면에 표시합니다.
//        scaleView.setImage(ImageSource.bitmap(mutableBitmap));
//
//        // 저장한 확대/축소 수준과 중심 좌표를 복원합니다.
//        scaleView.setScaleAndCenter(currentScale, currentCenter);
//
//        // 현재 이미지를 저장합니다.
//        currentBitmap = mutableBitmap;
//    }

    private void readCsvAndDrawIcons() {
        File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File csvFile = new File(dir, "coordinates.csv");

        try {
            FileReader fileReader = new FileReader(csvFile);
            BufferedReader bufferedReader = new BufferedReader(fileReader);

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                String[] coords = line.split(",");
                if (coords.length == 2) {
                    float x = Float.parseFloat(coords[0]);
                    float y = Float.parseFloat(coords[1]);
                    PointF point = new PointF(x, y);
                    drawCircleOnImage(point);
                }
            }

            bufferedReader.close();
            fileReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}// MainActivity.java