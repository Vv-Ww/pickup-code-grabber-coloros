package io.github.okaidev.pickupcode;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 取件码本地记录（桌面小组件数据源，v2.8 新增）：
 * 模块把待办写入系统日历的同时，在自身 App 私有存储记一份取件码摘要，
 * 供桌面小组件（PickupWidgetProvider）无 root 读取展示。
 *
 * 每条记录：code（取件码）、text（展示文本）、done（是否已取件）、ts（毫秒时间戳）。
 * 顺序：新码在前；容量上限 40 条。
 */
public class PickupStore {

    private static final String TAG = "PICKUPDEBUG";
    private static final String PREFS = "pickup_widget";
    private static final String KEY = "items";
    private static final int MAX = 40;

    /** 读取全部记录（新→旧） */
    public static List<Item> list(Context ctx) {
        List<Item> out = new ArrayList<>();
        try {
            String raw = prefs(ctx).getString(KEY, "[]");
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                Item it = new Item();
                it.code = o.optString("code");
                it.text = o.optString("text");
                it.done = o.optBoolean("done");
                it.ts = o.optLong("ts");
                if (it.code.isEmpty()) continue;
                out.add(it);
            }
        } catch (Throwable t) {
            Log.e(TAG, "PickupStore list err: " + t);
        }
        return out;
    }

    /** 新增一条（新码置顶；容量上限 40，超出丢最旧） */
    public static void record(Context ctx, String code, String text, long ts) {
        try {
            List<Item> all = list(ctx);
            for (int i = 0; i < all.size(); i++) {   // 同码只保留最新一条，避免刷屏
                if (code.equals(all.get(i).code)) { all.remove(i); break; }
            }
            Item it = new Item();
            it.code = code;
            it.text = text == null ? code : text;
            it.ts = ts;
            all.add(0, it);
            if (all.size() > MAX) all = new ArrayList<>(all.subList(0, MAX));
            save(ctx, all);
        } catch (Throwable t) {
            Log.e(TAG, "PickupStore record err: " + t);
        }
    }

    /** 按取件码标记已取件 */
    public static void markDone(Context ctx, String code) {
        try {
            List<Item> all = list(ctx);
            boolean changed = false;
            for (Item it : all) {
                if (code.equals(it.code) && !it.done) { it.done = true; changed = true; }
            }
            if (changed) save(ctx, all);
        } catch (Throwable t) {
            Log.e(TAG, "PickupStore markDone err: " + t);
        }
    }

    /** 删除指定取件码记录（应用内自定义删除/小组件取走） */
    public static void remove(Context ctx, String code) {
        try {
            List<Item> all = list(ctx);
            boolean changed = false;
            for (int i = all.size() - 1; i >= 0; i--) {
                if (code.equals(all.get(i).code)) { all.remove(i); changed = true; }
            }
            if (changed) save(ctx, all);
        } catch (Throwable t) {
            Log.e(TAG, "PickupStore remove err: " + t);
        }
    }

    /** 从展示文本中找含取件码的那条并标记（兜底：某些 markDone 只带 content 子串） */
    public static void markDoneByText(Context ctx, String codeOrText) {
        try {
            List<Item> all = list(ctx);
            boolean changed = false;
            for (Item it : all) {
                if (!it.done && (it.text.contains(codeOrText))) { it.done = true; changed = true; }
            }
            if (changed) save(ctx, all);
        } catch (Throwable t) {
            Log.e(TAG, "PickupStore markDoneByText err: " + t);
        }
    }


    /** 清空全部记录（一键删除用） */
    public static void clear(Context ctx) {
        try { prefs(ctx).edit().remove(KEY).apply(); } catch (Throwable t) { Log.e(TAG, "clear err: " + t); }
    }

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static void save(Context ctx, List<Item> all) {
        try {
            JSONArray arr = new JSONArray();
            for (Item it : all) {
                JSONObject o = new JSONObject();
                o.put("code", it.code);
                o.put("text", it.text);
                o.put("done", it.done);
                o.put("ts", it.ts);
                arr.put(o);
            }
            prefs(ctx).edit().putString(KEY, arr.toString()).apply();
        } catch (Throwable t) {
            Log.e(TAG, "PickupStore save err: " + t);
        }
    }

    /** 单条记录 */
    public static class Item {
        public String code = "";
        public String text = "";
        public boolean done = false;
        public long ts = 0L;
    }
}
