package io.github.okaidev.pickupcode;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 取件码桌面小组件（v2.15）：ListView 上下滑动浏览全部；
 * 行内「取走」直绑广播（尝试兼容 ColorOS 桌面）。
 */
public class PickupWidgetProvider extends AppWidgetProvider {

    public static final String ACTION_TAKE = "io.github.okaidev.pickupcode.WIDGET_TAKE";
    public static final String ACTION_OPEN = "io.github.okaidev.pickupcode.WIDGET_OPEN";
    public static final String EXTRA_CODE = "code";

    @Override
    public void onUpdate(Context context, AppWidgetManager mgr, int[] appWidgetIds) {
        for (int id : appWidgetIds) {
            try { updateOne(context, mgr, id); }
            catch (Throwable t) { android.util.Log.e("PICKUPDEBUG", "updateOne err", t); }
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (intent == null) return;
        if (ACTION_OPEN.equals(intent.getAction())) {
            try {
                Intent open = new Intent(context, PickupListActivity.class);
                open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(open);
            } catch (Throwable t) { android.util.Log.e("PICKUPDEBUG", "open list err", t); }
            return;
        }
        if (ACTION_TAKE.equals(intent.getAction())) {
            String code = intent.getStringExtra(EXTRA_CODE);
            android.util.Log.i("PICKUPDEBUG", "WIDGET-TAKE code=" + code);
            if (code != null && !code.isEmpty()) takeDone(context, code);
        }
    }

    private static void takeDone(Context context, String code) {
        try {
            Context app = context.getApplicationContext();
            if (PickupConfig.toCalendar(app)) {
                try { TodoWriter.runSql(app, NotesBackend.markDoneSql(app, code)); }
                catch (Throwable t) { android.util.Log.e("PICKUPDEBUG", "take cal err", t); }
            }
            PickupStore.markDone(app, code);
            PickupStore.markDoneByText(app, code);
            updateAll(app);
        } catch (Throwable t) {
            android.util.Log.e("PICKUPDEBUG", "widget take err", t);
        }
    }

    public static void updateAll(Context context) {
        try {
            Context app = context.getApplicationContext();
            AppWidgetManager mgr = AppWidgetManager.getInstance(app);
            int[] ids = mgr.getAppWidgetIds(new ComponentName(app, PickupWidgetProvider.class));
            if (ids == null) return;
            mgr.notifyAppWidgetViewDataChanged(ids, R.id.widget_list);
            for (int id : ids) updateOne(app, mgr, id);
        } catch (Throwable t) {
            android.util.Log.e("PICKUPDEBUG", "updateAll err", t);
        }
    }

    private static void updateOne(Context context, AppWidgetManager mgr, int id) {
        RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.widget_pickup);
        rv.setTextViewText(R.id.widget_time,
                new SimpleDateFormat("MM-dd HH:mm", Locale.CHINA).format(new Date()));
        Intent open = new Intent(context, PickupListActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        rv.setOnClickPendingIntent(R.id.widget_title_row,
                PendingIntent.getActivity(context, 1, open,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        // 点击任意列表行也打开记录页（template 无需携带行数据）
        Intent rowTpl = new Intent(context, PickupWidgetProvider.class);
        rowTpl.setAction(ACTION_OPEN);
        rv.setPendingIntentTemplate(R.id.widget_list,
                PendingIntent.getBroadcast(context, 77, rowTpl,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        Intent svc = new Intent(context, PickupWidgetService.class);
        rv.setRemoteAdapter(R.id.widget_list, svc);
        rv.setEmptyView(R.id.widget_list, R.id.widget_empty);
        mgr.updateAppWidget(id, rv);
    }
}
