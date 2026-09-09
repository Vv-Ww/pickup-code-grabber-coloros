package io.github.okaidev.pickupcode;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 取件码记录页（v2.9）：展示全部已捕获取件码（含已取走）。
 * 每条支持「取走」（标记完成并让组件消失）与「删除」（从记录中彻底移除）。
 */
public class PickupListActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(16), dp(18), dp(24));
        root.setBackgroundColor(Color.WHITE);
        render(root);
        ScrollView sv = new ScrollView(this);
        sv.addView(root);
        setContentView(sv);
    }

    private void render(LinearLayout root) {
        root.removeAllViews();
        TextView title = new TextView(this);
        title.setText("📋 取件码记录");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(Color.parseColor("#1A1A1A"));
        root.addView(title);

        List<PickupStore.Item> items = PickupStore.list(this);

        // v2.17：一键取走 / 一键删除
        LinearLayout bulk = new LinearLayout(this);
        bulk.setOrientation(LinearLayout.HORIZONTAL);
        bulk.setPadding(0, dp(6), 0, dp(6));
        bulk.addView(actBtn("一键取走", "#0B6E4F", v -> bulkTake(root, items)));
        bulk.addView(actBtn("一键删除", "#D9534F", v -> bulkDelete(root, items)));
        root.addView(bulk);

        if (items.isEmpty()) {
            root.addView(sp(10));
            TextView empty = new TextView(this);
            empty.setText("暂无记录\n收到取件短信后自动显示在这里");
            empty.setGravity(Gravity.CENTER);
            empty.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            empty.setTextColor(Color.parseColor("#999999"));
            root.addView(empty);
            return;
        }

        int pending = 0;
        for (PickupStore.Item it : items) if (!it.done) pending++;
        TextView stat = new TextView(this);
        stat.setText("共 " + items.size() + " 条 · 待取 " + pending + " 条（已取走的仍会显示，可随时删除）");
        stat.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        stat.setTextColor(Color.parseColor("#888888"));
        root.addView(stat);
        root.addView(sp(6));

        int idx = 0;
        for (PickupStore.Item it : items) {
            final PickupStore.Item item = it;
            root.addView(itemRow(item, () -> reRender(root)));
            idx++;
        }
    }

    private void reRender(LinearLayout root) { render(root); }

    private LinearLayout itemRow(PickupStore.Item it, Runnable refresh) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(10), dp(8), dp(10), dp(8));
        row.setBackgroundColor(it.done ? Color.parseColor("#F4F6FA")
                : Color.parseColor("#EEF6EF"));

        String time = " -";
        try {
            time = " · " + new SimpleDateFormat("MM-dd HH:mm", Locale.CHINA)
                    .format(new Date(it.ts));
        } catch (Throwable ignored) { }

        LinearLayout left = new LinearLayout(this);
        left.setOrientation(LinearLayout.VERTICAL);
        left.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        TextView line1 = new TextView(this);
        line1.setText((it.done ? "✅ " : "📦 ") + it.text);
        line1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        line1.setTextColor(it.done ? Color.parseColor("#88999999") : Color.parseColor("#222222"));
        left.addView(line1);
        TextView line2 = new TextView(this);
        line2.setText((it.done ? "已取走" : "待取") + time);
        line2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        line2.setTextColor(Color.parseColor("#999999"));
        left.addView(line2);
        row.addView(left);

        if (!it.done) {
            row.addView(actBtn("取走", "#0B6E4F", v -> {
                takeDone(it.code);
                Toast.makeText(this, "已标记取走：" + it.code, Toast.LENGTH_SHORT).show();
                refresh.run();
            }));
        }
        row.addView(actBtn("删除", "#D9534F", v -> {
            PickupStore.remove(this, it.code);
            PickupWidgetProvider.updateAll(this);
            Toast.makeText(this, "已删除：" + it.code, Toast.LENGTH_SHORT).show();
            refresh.run();
        }));

        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.VERTICAL);
        wrap.addView(row);
        wrap.addView(new TextView(this) {{ setBackgroundColor(Color.parseColor("#EEEEEE"));
            setHeight(dp(1)); }});
        return wrap;
    }

    /** 与小组件「取走」一致：日历标记完成 + 本地标记 */
    private void takeDone(String code) {
        try {
            if (PickupConfig.toCalendar(this)) {
                TodoWriter.runSql(this, NotesBackend.markDoneSql(this, code));
            }
            PickupStore.markDone(this, code);
            PickupStore.markDoneByText(this, code);
            PickupWidgetProvider.updateAll(this);
        } catch (Throwable t) {
            Toast.makeText(this, "取走失败：" + t.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private TextView actBtn(String text, String colorHex, android.view.View.OnClickListener l) {
        TextView b = new TextView(this);
        b.setText(text);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        b.setTextColor(Color.WHITE);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(10), dp(5), dp(10), dp(5));
        b.setBackgroundColor(Color.parseColor(colorHex));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dp(6), 0, 0, 0);
        b.setLayoutParams(lp);
        b.setOnClickListener(l);
        return b;
    }

    private LinearLayout sp(int h) {
        LinearLayout l = new LinearLayout(this);
        l.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(h)));
        return l;
    }


    /** 一键取走：全部未取走记录标记完成并刷新 */
    private void bulkTake(LinearLayout root, List<PickupStore.Item> items) {
        int n = 0;
        for (PickupStore.Item it : items) {
            if (!it.done) { takeDone(it.code); n++; }
        }
        Toast.makeText(this, "已取走 " + n + " 条", Toast.LENGTH_SHORT).show();
        render(root);
    }

    /** 一键删除：清空全部记录（本地记录；如需连日历待办也删除请手动） */
    private void bulkDelete(LinearLayout root, List<PickupStore.Item> items) {
        int n = items.size();
        if (n == 0) { Toast.makeText(this, "没有可删除的记录", Toast.LENGTH_SHORT).show(); return; }
        new android.app.AlertDialog.Builder(this)
                .setTitle("一键删除")
                .setMessage("确定删除全部 " + n + " 条记录？只影响本应用记录页/小组件，不影响日历待办。")
                .setPositiveButton("删除", (d, w) -> {
                    PickupStore.clear(this);
                    PickupWidgetProvider.updateAll(this);
                    Toast.makeText(this, "已删除 " + n + " 条", Toast.LENGTH_SHORT).show();
                    render(root);
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density + 0.5f); }
}
