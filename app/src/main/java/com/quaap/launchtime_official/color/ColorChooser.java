package com.quaap.launchtime_official.color;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import com.quaap.launchtime_official.R;

public class ColorChooser extends FrameLayout {


    private SeekBar colorRed;
    private SeekBar colorGreen;
    private SeekBar colorBlue;
    private SeekBar colorAlpha;
    private SeekBar colorBright;

    private GridLayout colorPresets;

    private TextView colorPreview;
    private SharedPreferences prefs;

    private ColorSelectedListener colorSelectedListener;

    public ColorChooser(Context context) {
        super(context);
        init();
    }

    public ColorChooser(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ColorChooser(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        ViewGroup frame = (ViewGroup) LayoutInflater.from(getContext()).inflate(R.layout.activity_color_chooser, this);

        colorRed = (SeekBar) frame.findViewById(R.id.color_red_seekbar);
        colorGreen = (SeekBar) frame.findViewById(R.id.color_green_seekbar);
        colorBlue = (SeekBar) frame.findViewById(R.id.color_blue_seekbar);
        colorAlpha = (SeekBar) frame.findViewById(R.id.color_alpha_seekbar);
        colorBright = (SeekBar) frame.findViewById(R.id.color_bright_seekbar);


        colorRed.setOnSeekBarChangeListener(colorChange);
        colorGreen.setOnSeekBarChangeListener(colorChange);
        colorBlue.setOnSeekBarChangeListener(colorChange);
        colorBright.setOnSeekBarChangeListener(colorChange);
        colorAlpha.setOnSeekBarChangeListener(colorChange);

        colorPreview = (TextView) frame.findViewById(R.id.color_preview);
        colorPreview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
             //   done();
            }
        });

        colorPresets = (GridLayout) frame.findViewById(R.id.color_presets);

        prefs = getContext().getSharedPreferences("colors", Context.MODE_PRIVATE);

        loadPresets();

        doPreview();
    }

    private void done() {
        int color = getSelectedColor();
        addPreset(color);

        if (colorSelectedListener!=null) {
            colorSelectedListener.colorSelected(color);
        }



//        Intent resultData = new Intent();
//        resultData.putExtra("color", color);
//        setResult(Activity.RESULT_OK, resultData);
//        finish();
    }

    private void doPreview() {
        int color = getSelectedColor();
        colorPreview.setBackgroundColor(color);
        int tcolor = ~color;
        colorPreview.setTextColor(Color.rgb(Color.red(tcolor), Color.green(tcolor), Color.blue(tcolor)));

    }

    public void setColor(int color) {

        if (Build.VERSION.SDK_INT >= 24) {
            colorAlpha.setProgress(255 - Color.alpha(color), true);
            colorBright.setProgress(255, true);

            colorRed.setProgress(Color.red(color), true);
            colorGreen.setProgress(Color.green(color), true);
            colorBlue.setProgress(Color.blue(color), true);
        } else {

            animateProgress(colorAlpha, 255 - Color.alpha(color));
            animateProgress(colorBright, 255);

            animateProgress(colorRed, Color.red(color));
            animateProgress(colorGreen, Color.green(color));
            animateProgress(colorBlue, Color.blue(color));

        }
    }

    //Timer panimtimer = new Timer();

    private void animateProgress(final ProgressBar b, int newval) {
        b.setProgress(newval);
    }


    private void animateProgress2(final ProgressBar b, int newval) {
        int cval = b.getProgress();

        int i = 0;
        while (cval != newval && i < 255) { //'i' is a safety in case there's a miss
            cval += Math.signum(newval - cval);

            final int prog = cval;
            b.postDelayed(new Runnable() {
                @Override
                public void run() {
                    b.setProgress(prog);
                }
            }, i++);
        }
        b.setProgress(newval);
    }

    public int getSelectedColor() {
        int alpha = 255 - colorAlpha.getProgress();
        float bright = colorBright.getProgress() / 255f;

        int red = (int) (colorRed.getProgress() * bright);
        int green = (int) (colorGreen.getProgress() * bright);
        int blue = (int) (colorBlue.getProgress() * bright);

        return Color.argb(alpha, red, green, blue);
    }

    private SeekBar.OnSeekBarChangeListener colorChange = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
            doPreview();
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }
    };

    private View.OnClickListener setColorListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            setColor((int) view.getTag());

        }
    };

    int numpresets = 12;

    private void addPreset(int color) {
        SharedPreferences.Editor edit = prefs.edit();
        // edit.clear();

        int top = numpresets - 2;
        for (int i = top; i >= 0; i--) {
            int oldcolor = prefs.getInt("color" + i, Integer.MIN_VALUE);
            if (oldcolor == color) {
                top = i - 1;
                break;
            }
        }

        for (int i = top; i >= 0; i--) {
            int oldcolor = prefs.getInt("color" + i, Integer.MIN_VALUE);
            if (color != Integer.MIN_VALUE) {
                edit.putInt("color" + (i + 1), oldcolor);
            }

        }
        edit.putInt("color0", color);
        edit.apply();

    }

    private void loadPresets() {

        int[] colors = {Color.BLACK, Color.WHITE, Color.DKGRAY,
                Color.GRAY, Color.LTGRAY, Color.RED,
                Color.rgb(255, 127, 0), Color.YELLOW, Color.GREEN,
                Color.BLUE, Color.rgb(127, 0, 255), Color.rgb(255, 0, 255)};


        for (int i = 0; i < numpresets; i++) {
            int color = prefs.getInt("color" + i, Integer.MIN_VALUE);
            if (color != Integer.MIN_VALUE) {
                makeColorPresetButton(color);
            }

        }

        for (int color : colors) {
            makeColorPresetButton(color);
        }

    }

    private void makeColorPresetButton(int color) {
        TextView c = new TextView(getContext());
        c.setText("   ");
        c.setTextSize(28);
        c.setBackgroundColor(color);
        c.setTag(color);
        c.setClickable(true);
        c.setOnClickListener(setColorListener);
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.setMargins(16, 16, 16, 16);
        colorPresets.addView(c, lp);
    }

    interface ColorSelectedListener {
        void colorSelected(int color);
    }
}

