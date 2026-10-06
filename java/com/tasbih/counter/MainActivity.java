package com.tasbih.counter;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.InputType;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

public class MainActivity extends AppCompatActivity {

    private int count = 0;
    private boolean isTasbihActive = false;
    private final String PIN_PASSWORD = "1234";

    private TextView tvCount;
    private SwitchCompat switchActive;
    private Vibrator vibrator;

    private boolean isUpPressed = false;
    private boolean isDownPressed = false;
    private final Handler resetHandler = new Handler(Looper.getMainLooper());
    private Runnable resetRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            );
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        FrameLayout rootLayout = new FrameLayout(this);
        rootLayout.setBackgroundColor(Color.BLACK);

        LinearLayout contentLayout = new LinearLayout(this);
        contentLayout.setOrientation(LinearLayout.VERTICAL);
        contentLayout.setGravity(Gravity.CENTER);

        switchActive = new SwitchCompat(this);
        switchActive.setText("તસબીહ મોડ  ");
        switchActive.setTextColor(Color.GRAY);
        switchActive.setTextSize(18);

        tvCount = new TextView(this);
        tvCount.setText("0");
        tvCount.setTextColor(Color.WHITE);
        tvCount.setTextSize(96);
        tvCount.setGravity(Gravity.CENTER);
        tvCount.setPadding(0, 80, 0, 80);

        TextView tvHint = new TextView(this);
        tvHint.setText("વોલ્યુમ અથવા સ્ક્રીન ટચથી કાઉન્ટ થશે\nબંને વોલ્યુમ બટન 5 સેકન્ડ દબાવવાથી રિસેટ થશે");
        tvHint.setTextColor(Color.DKGRAY);
        tvHint.setTextSize(14);
        tvHint.setGravity(Gravity.CENTER);

        contentLayout.addView(switchActive);
        contentLayout.addView(tvCount);
        contentLayout.addView(tvHint);

        rootLayout.addView(contentLayout, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT,
            Gravity.CENTER
        ));

        setContentView(rootLayout);

        rootLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (isTasbihActive && event.getAction() == MotionEvent.ACTION_DOWN) {
                    incrementCount();
                    return true;
                }
                return false;
            }
        });

        switchActive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (switchActive.isChecked()) {
                    isTasbihActive = true;
                    applyScreenMode(true);
                } else {
                    switchActive.setChecked(true);
                    showPinDialog();
                }
            }
        });

        resetRunnable = new Runnable() {
            @Override
            public void run() {
                if (isUpPressed && isDownPressed) {
                    count = 0;
                    updateUI();
                    triggerDoubleBuzz();
                }
            }
        };
    }

    private void incrementCount() {
        count++;
        updateUI();

        if (count % 100 == 0) {
            triggerVibration(1000);
        } else {
            triggerVibration(25);
        }
    }

    private void updateUI() {
        tvCount.setText(String.valueOf(count));
    }

    private void applyScreenMode(boolean active) {
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        if (active) {
            lp.screenBrightness = 0.3f;
        } else {
            lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
        }
        getWindow().setAttributes(lp);
    }

    private void showPinDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("મોડ બંધ કરવા પિન દાખલ કરો");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        builder.setView(input);

        builder.setPositiveButton("OK", (dialog, which) -> {
            String enteredPin = input.getText().toString();
            if (enteredPin.equals(PIN_PASSWORD)) {
                isTasbihActive = false;
                switchActive.setChecked(false);
                applyScreenMode(false);
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (!isTasbihActive) {
            return super.onKeyDown(keyCode, event);
        }

        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) isUpPressed = true;
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) isDownPressed = true;

        if (isUpPressed && isDownPressed) {
            resetHandler.postDelayed(resetRunnable, 5000);
            return true;
        }

        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (event.getRepeatCount() == 0) {
                incrementCount();
            }
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) isUpPressed = false;
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) isDownPressed = false;

        resetHandler.removeCallbacks(resetRunnable);

        if (isTasbihActive && (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)) {
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    private void triggerVibration(long ms) {
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(ms);
            }
        }
    }

    private void triggerDoubleBuzz() {
        if (vibrator != null && vibrator.hasVibrator()) {
            long[] pattern = {0, 150, 100, 150};
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1));
            } else {
                vibrator.vibrate(pattern, -1);
            }
        }
    }
}
