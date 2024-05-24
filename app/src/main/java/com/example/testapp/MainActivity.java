package com.example.testapp;

import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PointF;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Bundle;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fab= findViewById(R.id.fab);
        frameLayout = findViewById(R.id.frameLayout);
        nfcTitle = findViewById(R.id.nfcTitle);
//        imageView = findViewById(R.id.imageView);
        scaleView = findViewById(R.id.imageView);

        scaleView.setImage(ImageSource.resource(R.drawable.test));

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
//            if(!isCheck){
//                scaleGestureDetector.onTouchEvent(motionEvent);
//            }
//            if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
//                PointF imageCoord = new PointF(motionEvent.getX(), motionEvent.getY());
//                if(isCheck) {
//                    drawIconOnImage(imageCoord);
//                }
//            }
//            return true;
//        });

//        scaleView.setOnTouchListener((view, motionEvent) -> {
//            if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
//                // 터치 이벤트가 끝났을 때
//                PointF viewCoord = new PointF(motionEvent.getX(), motionEvent.getY());
//                PointF imageCoord = scaleView.viewToSourceCoord(viewCoord);
//                if(isCheck) {
//                    // 아이콘 그리기 동작을 수행
//                    drawIconOnImage(imageCoord); // 화면 좌표를 사용하여 아이콘 그리기
//                }
//            }
//            return false; // onTouch 이벤트를 여기서 끝내지 않고, 다음 이벤트로 넘깁니다.
//        });

        scaleView.setOnTouchListener((view, motionEvent) -> {
            if (isCheck && motionEvent.getAction() == MotionEvent.ACTION_UP) {
                // 터치 이벤트가 끝났을 때
                PointF viewCoord = new PointF(motionEvent.getX(), motionEvent.getY());
                PointF imageCoord = scaleView.viewToSourceCoord(viewCoord);

                // 현재의 확대/축소 수준과 중심 좌표를 저장합니다.
                float currentScale = scaleView.getScale();
                PointF currentCenter = scaleView.getCenter();

                // 확대/축소를 비활성화합니다.
                scaleView.setZoomEnabled(false);

                // 중심 좌표를 다시 설정합니다.
                scaleView.setScaleAndCenter(currentScale, currentCenter);

                // 아이콘 그리기 동작을 수행
                drawIconOnImage(imageCoord); // 화면 좌표를 사용하여 아이콘 그리기
            }
            return false; // onTouch 이벤트를 여기서 끝내지 않고, 다음 이벤트로 넘깁니다.
        });


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
        for (int i = frameLayout.getChildCount() - 1; i >= 0; i--) {
            View child = frameLayout.getChildAt(i);
            if (child instanceof ImageView && child != imageView) {
                frameLayout.removeViewAt(i);
            }
        }
    }

    private void drawIconOnImage(PointF point) {
        // 아이콘 이미지 로드
        Bitmap iconBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.icon);

        // 아이콘을 ImageView로 생성
        ImageView iconView = new ImageView(this);
        iconView.setImageBitmap(iconBitmap);

        // 이미지의 좌표를 화면의 좌표로 변환
        PointF viewCoord = scaleView.sourceToViewCoord(point);

        // sourceToViewCoord가 null을 반환하는 경우를 처리
        if (viewCoord == null) {
            // 아이콘을 화면에 표시하지 않음
            return;
        }

        // 아이콘의 위치 설정
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(80, 80);
        layoutParams.leftMargin = (int) viewCoord.x - layoutParams.width / 2;
        layoutParams.topMargin = (int) viewCoord.y - layoutParams.height / 2;

        // FrameLayout에 아이콘 추가
        frameLayout.addView(iconView, layoutParams);
    }


}
