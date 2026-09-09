package io.github.okaidev.pickupcode;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.TypedValue;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import java.util.ArrayList;
import java.util.List;

public class PickupWidgetService extends RemoteViewsService {

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new Factory(getApplicationContext());
    }

    private static class Factory implements RemoteViewsService.RemoteViewsFactory {
        private final Context ctx;
        private final List<PickupStore.Item> items = new ArrayList<>();

        Factory(Context c) { ctx = c; }
        @Override public void onCreate() { }
        @Override
        public void onDataSetChanged() {
            items.clear();
            for (PickupStore.Item it : PickupStore.list(ctx)) if (!it.done) items.add(it);
        }
        @Override public void onDestroy() { }
        @Override public int getCount() { return items.size(); }

        @Override
        public RemoteViews getViewAt(int position) {
            PickupStore.Item it = items.get(position);
            RemoteViews rv = new RemoteViews(ctx.getPackageName(), R.layout.widget_row_pickup);
            float sp = PickupConfig.widgetFont(ctx);
            String line = lineOf(it.text);
            rv.setTextViewText(R.id.row_text, line);
            rv.setTextViewTextSize(R.id.row_text, TypedValue.COMPLEX_UNIT_SP, sp);
            return rv;
        }

        private String lineOf(String text) {
            String s = text == null ? "" : text;
            int tail = s.lastIndexOf('｜');
            if (tail > 0 && tail > s.length() - 20) s = s.substring(0, tail);
            return s.trim();
        }

        @Override public RemoteViews getLoadingView() { return null; }
        @Override public int getViewTypeCount() { return 1; }
        @Override public long getItemId(int p) { return p; }
        @Override public boolean hasStableIds() { return false; }
    }
}
