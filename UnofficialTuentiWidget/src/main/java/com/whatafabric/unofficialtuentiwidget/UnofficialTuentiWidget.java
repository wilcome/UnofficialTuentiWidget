/**
 * Created by Enrique García Orive on 21/05/14.
 */

package com.whatafabric.unofficialtuentiwidget;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.RemoteViews;

import java.io.File;
import java.util.HashMap;


/**
 * Implementation of App Widget functionality.
 * App Widget Configuration implemented in {@link UnofficialTuentiWidgetConfigureActivity UnofficialTuentiWidgetConfigureActivity}
 */
public class UnofficialTuentiWidget extends AppWidgetProvider {

    public static final String UPDATE = "com.whatafabric.unofficialtuentiwidget.UPDATE_WIDGET";
    public static final String FORCE_UPDATE = "com.whatafabric.unofficialtuentiwidget.FORCE_UPDATE_WIDGET";
    public Context context;
    private static HashMap<Integer, Uri> uris = new HashMap<Integer, Uri>();

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        Log.d("UnofficialTuentiWidget:onUpdate ", "begin");
        File file = new File(context.getDir("data",
                UnofficialTuentiWidgetConfigureActivity.MODE_PRIVATE),
                UnofficialTuentiWidgetConfigureActivity.FILENAME);
        if(file.exists()){
            this.context = context;
            final int N = appWidgetIds.length;
            for (int i=0; i<N; i++) {
                updateAppWidget(context, appWidgetManager, appWidgetIds[i]);
            }
        }
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        // When the user deletes the widget, delete the preference associated with it.
        for (int appWidgetId : appWidgetIds)
        {
            UnofficialTuentiWidgetConfigureActivity.deleteData(context, appWidgetId);
            cancelAlarmManager(context, appWidgetId);
        }
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }



    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    @Override
    public void onReceive(Context context,Intent intent)
    {
        String action = intent.getAction();
        Log.d("UnofficialTuentiWidget:onReceive", "action: " + action);
        if(action.equals(AppWidgetManager.ACTION_APPWIDGET_UPDATE) ||
                action.equals(UPDATE) || action.equals(FORCE_UPDATE))
        {
            //Check if there is a single widget ID.
            int widgetID = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
            Log.d("UnofficialTuentiWidget:onReceive","WIDGETID -*-*-*-*-*-*-> "+widgetID);
            //If there is no single ID, call the super implementation.
            if(widgetID == AppWidgetManager.INVALID_APPWIDGET_ID)
                super.onReceive(context, intent);
                //Otherwise call our onUpdate() passing a one element array, with the retrieved ID.
            else {
                Log.d("UnofficialTuentiWidget:onReceive", "kernel update");
                this.onUpdate(context, AppWidgetManager.getInstance(context), new int[]{widgetID});
            }
        }
        else
            super.onReceive(context, intent);
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {
        Log.d("UnofficialTuentiWidget:updateAppWidget ","begin");
        HashMap<String, String> dataMap = UnofficialTuentiWidgetConfigureActivity.loadData(context, appWidgetId);
        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.unofficial_tuenti_widget);
        NetworkTask nt = new NetworkTask(context,views,appWidgetManager,appWidgetId);
        nt.execute(dataMap);
    }

    public static void addUri(int id, Uri uri)
    {
        uris.put(new Integer(id), uri);
    }

    protected void cancelAlarmManager(Context context, int widgetID)
    {
        Log.d("UnofficialTuentiWidget:cancelAlarmManager ","begin");
        AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intentUpdate = new Intent(context, UnofficialTuentiWidget.class);
        //AlarmManager are identified with Intent's Action and Uri.
        intentUpdate.setAction(UPDATE);
        //For a global AlarmManager, don't put the uri to cancel
        //all the AlarmManager with action UPDATE_ONE.
        intentUpdate.setData(uris.get(widgetID));
        intentUpdate.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetID);
        PendingIntent pendingIntentAlarm = PendingIntent.getBroadcast(context,
                widgetID,
                intentUpdate,
                PendingIntent.FLAG_UPDATE_CURRENT);

        alarm.cancel(pendingIntentAlarm);
        Log.d("cancelAlarmManager", "Cancelled Alarm. Action = " +
                UnofficialTuentiWidget.UPDATE +
                " URI = " + uris.get(widgetID));
        uris.remove(widgetID);
    }
}


