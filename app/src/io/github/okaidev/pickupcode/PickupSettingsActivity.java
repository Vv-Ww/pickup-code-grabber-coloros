package io.github.okaidev.pickupcode;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * 小组件 / 写入目标设置（v2.10）：
 *  - 写入目标：待办(日历) / 小组件 / 两者都写
 *  - 正文字号：输入即预览（下方实例实时跟随）
 *  - 组件尺寸：小/中/大 预设（保存即生效），桌面长按组件还可手动微调
 * 首次添加小组件时由桌面打开（android:configure），也可从 App 设置进入。
 */
public class PickupSettingsActivity extends Activity {

    private String target = PickupConfig.TARGET_BOTH;
    private TextView btnBoth, btnCalendar, btnWidget;
    private EditText fontEt;
    private TextView preview;
    private int appWidgetId = android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        target = PickupConfig.writeTarget(this);
        float font = PickupConfig.widgetFont(this);
        // 桌面添加组件时会以 configure 启动本页；完成时需回 RESULT_OK 组件才会真正放上桌面
        appWidgetId = getIntent().getIntExtra(
                android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID,
                android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(18), dp(20), dp(20));
        root.setBackgroundColor(Color.WHITE);

        TextView title = new TextView(this);
        title.setText("⚙️ 取件码设置");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(Color.parseColor("#1A1A1A"));
        root.addView(title);

        root.addView(space(12));
        root.addView(label("取件码写到哪里"));
        root.addView(space(6));
        LinearLayout seg = new LinearLayout(this);
        seg.setOrientation(LinearLayout.HORIZONTAL);
        btnBoth = segBtn("待办+组件", PickupConfig.TARGET_BOTH);
        btnCalendar = segBtn("仅待办", PickupConfig.TARGET_CALENDAR);
        btnWidget = segBtn("仅组件", PickupConfig.TARGET_WIDGET);
        seg.addView(btnBoth); seg.addView(btnCalendar); seg.addView(btnWidget);
        root.addView(seg);

        root.addView(space(18));
        root.addView(label("小组件正文字号"));
        root.addView(space(6));
        fontEt = new EditText(this);
        fontEt.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        fontEt.setText(String.valueOf((int) font));
        fontEt.setSingleLine(true);
        fontEt.setPadding(dp(10), dp(8), dp(10), dp(8));
        fontEt.setBackgroundColor(Color.parseColor("#F0F3F8"));
        root.addView(fontEt);

        root.addView(space(8));
        root.addView(label("字号预览（即时生效）"));
        root.addView(space(4));
        preview = new TextView(this);
        preview.setPadding(dp(10), dp(8), dp(10), dp(8));
        preview.setBackgroundColor(Color.parseColor("#F7F9FC"));
        preview.setText("📦 取件码 8-6-4321｜菜鸟驿站｜多多驿站");
        preview.setTextColor(Color.parseColor("#333333"));
        preview.setTextSize(TypedValue.COMPLEX_UNIT_SP, font);
        root.addView(preview);
        fontEt.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                try {
                    float v = Float.parseFloat(s.toString());
                    preview.setTextSize(TypedValue.COMPLEX_UNIT_SP, v);
                } catch (Throwable ignored) { }
            }
            public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            public void onTextChanged(CharSequence s, int a, int b, int c) { }
        });

        root.addView(space(18));
        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        TextView save = new TextView(this);
        save.setText("💾 保存并放置");
        save.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        save.setTextColor(Color.WHITE);
        save.setGravity(Gravity.CENTER);
        save.setPadding(dp(16), dp(12), dp(16), dp(12));
        save.setBackgroundColor(Color.parseColor("#1E6FE8"));
        save.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        save.setOnClickListener(v -> {
            float sp = 12f;
            try { sp = Float.parseFloat(fontEt.getText().toString().trim()); } catch (Throwable ignored) { }
            PickupConfig.setWidgetFont(this, sp);
            PickupConfig.setWriteTarget(this, target);
            PickupWidgetProvider.updateAll(this);
            // configure 正确返回：携带 widget id + RESULT_OK，否则组件会被系统当作“取消”而消失
            android.content.Intent result = new android.content.Intent();
            result.putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            setResult(RESULT_OK, result);
            finish();
        });
        TextView cancel = new TextView(this);
        cancel.setText("取消");
        cancel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        cancel.setTextColor(Color.parseColor("#333333"));
        cancel.setGravity(Gravity.CENTER);
        cancel.setPadding(dp(16), dp(12), dp(16), dp(12));
        cancel.setBackgroundColor(Color.parseColor("#E0E0E0"));
        cancel.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        cancel.setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });
        btnRow.addView(cancel);
        btnRow.addView(save);
        root.addView(btnRow);

        ScrollView sv = new ScrollView(this);
        sv.addView(root);
        setContentView(sv);
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_CANCELED);
        super.onBackPressed();
    }

    private TextView segBtn(String text, String val) {
        TextView b = new TextView(this);
        b.setText(text);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(4), dp(10), dp(4), dp(10));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        lp.setMargins(dp(2), 0, dp(2), 0);
        b.setLayoutParams(lp);
        b.setTag(val);
        b.setOnClickListener(v -> {
            String val2 = (String) v.getTag();
            if (val2.equals(PickupConfig.TARGET_BOTH) || val2.equals(PickupConfig.TARGET_CALENDAR)
                    || val2.equals(PickupConfig.TARGET_WIDGET)) {
                target = val2;
            }
            restyle();
        });
        return b;
    }

    private void restyle() {
        styleSeg(btnBoth, target, PickupConfig.TARGET_BOTH);
        styleSeg(btnCalendar, target, PickupConfig.TARGET_CALENDAR);
        styleSeg(btnWidget, target, PickupConfig.TARGET_WIDGET);
    }

    private void styleSeg(TextView b, String cur, String mine) {
        boolean on = mine.equals(cur);
        b.setTextColor(on ? Color.WHITE : Color.parseColor("#333333"));
        b.setBackgroundColor(on ? Color.parseColor("#1E6FE8") : Color.parseColor("#EDF1F7"));
    }

    private TextView label(String s) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        t.setTextColor(Color.parseColor("#1A1A1A"));
        t.setTypeface(null, Typeface.BOLD);
        return t;
    }

    private LinearLayout space(int h) {
        LinearLayout l = new LinearLayout(this);
        l.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(h)));
        return l;
    }

    private int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density + 0.5f); }
}
