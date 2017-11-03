package com.quaap.launchtime.color;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import com.quaap.launchtime.GlobState;
import com.quaap.launchtime.R;

import java.util.regex.Pattern;

public class ColorChooser extends FrameLayout {


    private SeekBar colorRed;
    private SeekBar colorGreen;
    private SeekBar colorBlue;
    private SeekBar colorAlpha;
    private SeekBar colorBright;

    private GridLayout colorPresets;

    private TextView colorPreview;
    private EditText colorHex;

    private SharedPreferences prefs;

    private ColorSelectedListener colorSelectedListener;


    public ColorChooser(Context context) {
        super(context);

        init(GlobState.getIconsHandler(context).getIconsPackPackageName());
    }

//    public ColorChooser(Context context, AttributeSet attrs) {
//        super(context, attrs);
//        init();
//    }
//
//    public ColorChooser(Context context, AttributeSet attrs, int defStyleAttr) {
//        super(context, attrs, defStyleAttr);
//        init();
//    }

    private void init(String themename) {
        ViewGroup frame = (ViewGroup) LayoutInflater.from(getContext()).inflate(R.layout.activity_color_chooser, this);

        colorRed = frame.findViewById(R.id.color_red_seekbar);
        colorGreen = frame.findViewById(R.id.color_green_seekbar);
        colorBlue = frame.findViewById(R.id.color_blue_seekbar);
        colorAlpha = frame.findViewById(R.id.color_alpha_seekbar);
        colorBright = frame.findViewById(R.id.color_bright_seekbar);
        colorHex = frame.findViewById(R.id.color_hex);


        colorRed.setOnSeekBarChangeListener(colorChange);
        colorGreen.setOnSeekBarChangeListener(colorChange);
        colorBlue.setOnSeekBarChangeListener(colorChange);
        colorBright.setOnSeekBarChangeListener(colorChange);
        colorAlpha.setOnSeekBarChangeListener(colorChange);


        colorHex.setCursorVisible(false);
        colorHex.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                checkEditorColor();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        colorHex.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                //Log.d("ColorChooser", " " + colorHex.getText());
                Integer color = checkEditorColor();
                if (color!=null) {
                    setColor(color);
                    colorHex.setCursorVisible(false);
                    return false;
                }
                return true;
            }
        });
        colorHex.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                colorHex.setCursorVisible(true);
            }
        });

        colorPreview = frame.findViewById(R.id.color_preview);
        colorPreview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
             //   done();
            }
        });

        colorPresets = frame.findViewById(R.id.color_presets);

        prefs = getContext().getSharedPreferences("colors" + themename, Context.MODE_PRIVATE);

        loadPresets();

        doPreview();
    }

    private Integer checkEditorColor() {
        Integer color;
        try {
            String text = colorHex.getText().toString().trim();
            if (Pattern.matches("(?xi: [0-9a-f]{6} | [0-9a-f]{8} )", text)) {
                text = "#" + text;
            }
            color = Color.parseColor(text);
            colorHex.setTextColor(Color.GREEN);
        } catch (Exception e) {
            colorHex.setTextColor(Color.RED);
            color = null;
        }
        return color;
    }

    public void done() {
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
        colorHex.setText(String.format("#%08X", color));
        colorPreview.setBackgroundColor(color);
        int tcolor = ~color;
        colorPreview.setTextColor(Color.rgb(Color.red(tcolor), Color.green(tcolor), Color.blue(tcolor)));

    }

    public void setColor(int color) {

        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        int bright = 255;

//        int top = bright - Math.max(Math.max(red,green), blue);
//        bright -= top/3;
//        red += top*red/255.0;
//        green += top*green/255.0;
//        blue += top*blue/255.0;

        if (Build.VERSION.SDK_INT >= 24) {
            colorAlpha.setProgress(255 - Color.alpha(color), true);
            colorBright.setProgress(bright, true);

            colorRed.setProgress(red, true);
            colorGreen.setProgress(green, true);
            colorBlue.setProgress(blue, true);
        } else {

            animateProgress(colorAlpha, 255 - Color.alpha(color));
            animateProgress(colorBright, bright);

            animateProgress(colorRed, red);
            animateProgress(colorGreen, green);
            animateProgress(colorBlue, blue);

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
            colorHex.setCursorVisible(false);
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

    int numpresets = 64;

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

        int[] colors = {Color.TRANSPARENT, Color.BLACK, Color.WHITE, Color.DKGRAY,
                Color.LTGRAY, Color.RED,
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
        FrameLayout outframe = new FrameLayout(getContext());
        outframe.setBackgroundColor(Color.BLACK);
        outframe.setPadding(6,6,6,6);

        FrameLayout frame = new FrameLayout(getContext());
        //frame.setBackgroundColor(Color.BLACK);
        frame.setBackgroundResource(R.drawable.transparentgrid);

        TextView c = new TextView(getContext());
        c.setText("   ");
        c.setTextSize(22);
        c.setBackgroundColor(color);
//        if (color==Color.TRANSPARENT) {
//            c.setBackgroundResource(R.drawable.transparentgrid);
//        }
        c.setTag(color);
        c.setClickable(true);
        c.setOnClickListener(setColorListener);
        frame.addView(c);
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.setMargins(24, 16, 24, 16);
        outframe.setPadding(6,6,6,6);
        outframe.addView(frame);
        colorPresets.addView(outframe, lp);
    }

    interface ColorSelectedListener {
        void colorSelected(int color);
    }
}

