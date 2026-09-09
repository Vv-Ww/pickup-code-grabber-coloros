package io.github.okaidev.pickupcode;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * 小组件/写入目标配置（v2.8）：
 *  - write_target：取件码写到哪  both(待办+小组件, 默认) | calendar(仅待办) | widget(仅小组件)
 *  - widget_font：小组件正文字号(sp)
 */
public class PickupConfig {

    public static final String TARGET_BOTH = "both";
    public static final String TARGET_CALENDAR = "calendar";
    public static final String TARGET_WIDGET = "widget";

    private static final String PREFS = "pickup_config";
    private static final String KEY_TARGET = "write_target";
    private static final String KEY_FONT = "widget_font";

    public static String writeTarget(Context ctx) {
        return prefs(ctx).getString(KEY_TARGET, TARGET_BOTH);
    }

    public static void setWriteTarget(Context ctx, String t) {
        if (t == null) t = TARGET_BOTH;
        prefs(ctx).edit().putString(KEY_TARGET, t).apply();
    }

    public static boolean toCalendar(Context ctx) {
        String t = writeTarget(ctx);
        return TARGET_CALENDAR.equals(t) || TARGET_BOTH.equals(t);
    }

    public static boolean toWidgetStore(Context ctx) {
        String t = writeTarget(ctx);
        return TARGET_WIDGET.equals(t) || TARGET_BOTH.equals(t);
    }

    public static float widgetFont(Context ctx) {
        return prefs(ctx).getFloat(KEY_FONT, 12f);
    }

    public static void setWidgetFont(Context ctx, float sp) {
        if (sp < 9f) sp = 9f;
        if (sp > 24f) sp = 24f;
        prefs(ctx).edit().putFloat(KEY_FONT, sp).apply();
    }

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
