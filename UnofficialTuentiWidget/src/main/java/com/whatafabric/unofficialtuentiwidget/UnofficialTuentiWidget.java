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
import android.content.res.Configuration;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import android.widget.RemoteViews;

import java.io.File;
import java.util.HashMap;


/**
 * Implementation of App Widget functionality.
 * App Widget Configuration implemented in {@link UnofficialTuentiWidgetConfigureActivity UnofficialTuentiWidgetConfigureActivity}
 */
public class UnofficialTuentiWidget extends AppWidgetProvider {
    private static boolean LOGGING = true;
    public static final String UPDATE_WIDGET = "com.whatafabric.unofficialtuentiwidget.UPDATE_WIDGET";
    public static final String FORCE_UPDATE_WIDGET = "com.whatafabric.unofficialtuentiwidget.FORCE_UPDATE_WIDGET";
    public Context context;
    private static int squareSide; //dp in xdpi

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        if (LOGGING) Log.d("UTuentiW,UnofficialTuentiWidget:onUpdate ", "begin");
        this.context = context;
        final int N = appWidgetIds.length;
        boolean noFile = true;
        for (int i=0; i<N; i++) {
            File file = new File(context.getDir("data",
                    UnofficialTuentiWidgetConfigureActivity.MODE_PRIVATE),
                    UnofficialTuentiWidgetConfigureActivity.FILENAME + "_" + appWidgetIds[i]);
            if(file.exists()) {
                noFile = false;
                updateAppWidget(context, appWidgetManager, appWidgetIds[i], false);
            }
        }

        if(noFile){
            for (int i=0; i<N; i++) {
                if (LOGGING) Log.d("UTuentiW,UnofficialTuentiWidget:onUpdate ", "noNewFile case - appWidgetId = " + appWidgetIds[i]);
                UnofficialTuentiWidgetConfigureActivity.loadData(context, appWidgetIds[i]);
                updateAppWidget(context, appWidgetManager, appWidgetIds[i], false);
            }
        }

    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        // When the user deletes the widget, delete the preferences associated with it.
        if (LOGGING) Log.d("UTuentiW,UnofficialTuentiWidget:onEnabled", "Begin");

        for (int appWidgetId : appWidgetIds)
        {
            if (LOGGING) Log.d("UTuentiW,UnofficialTuentiWidget:onEnabled", "Delete appWidgetId = " + appWidgetId);
            UnofficialTuentiWidgetConfigureActivity.deleteData(context, appWidgetId);
            cancelAlarmManager(context, appWidgetId);
        }
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
        if (LOGGING) Log.d("UTuentiW,UnofficialTuentiWidget:onEnabled", "Begin");
        squareSide = Math.round(40 * (context.getResources().getDisplayMetrics().xdpi / DisplayMetrics.DENSITY_DEFAULT));
        if (LOGGING) Log.d("UTuentiW,UnofficialTuentiWidget:onEnabled", "squareSide set to: "+squareSide);
    }



    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
        if (LOGGING) Log.d("UTuentiW,UnofficialTuentiWidget:onDisabled", "Begin");

    }


    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager,
                                          int appWidgetId, Bundle newOptions) {
        if (LOGGING) Log.d("UTuentiW,UnofficialTuentiWidget:onAppWidgetOptionsChanged", "Begin");
        //int minWidth = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
        int maxWidth = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH);
        //int minHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT);
        int maxHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT);
        int dp = maxWidth < maxHeight ? maxWidth : maxHeight;
        if (LOGGING) Log.d("UTuentiW,UnofficialTuentiWidget:onAppWidgetOptionsChanged", "dp =" + dp);
        squareSide = Math.round(dp * (context.getResources().getDisplayMetrics().xdpi / DisplayMetrics.DENSITY_DEFAULT));
        //this.onUpdate(context, AppWidgetManager.getInstance(context), new int[]{appWidgetId});
        updateAppWidget(context, appWidgetManager, appWidgetId, true);
    }

    @Override
    public void onReceive(Context context,Intent intent)
    {
        String action = intent.getAction();
        if (LOGGING) Log.d("UTuentiW,UnofficialTuentiWidget:onReceive", "action: " + action);

        if(action.equals(AppWidgetManager.ACTION_APPWIDGET_UPDATE) ||
                action.equals(UPDATE_WIDGET) || action.equals(FORCE_UPDATE_WIDGET))
        {
            //Check if there is a single widget ID.
            int widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                                              AppWidgetManager.INVALID_APPWIDGET_ID);

            if (LOGGING) Log.d("UTuentiW,UnofficialTuentiWidget:onReceive", "widgetId: " + widgetId);
            //If there is no single ID, call the super implementation.
            if(widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
                super.onReceive(context, intent);
                //Otherwise call our onUpdate() passing a one element array, with the retrieved ID.
            }else if (action.equals(FORCE_UPDATE_WIDGET)) {
                if (LOGGING) Log.d("UTuentiW,UnofficialTuentiWidget:onReceive", "force update");
                this.onUpdate(context, AppWidgetManager.getInstance(context), new int[]{widgetId});
            }else if(action.equals(UPDATE_WIDGET)){
                if (LOGGING) Log.d("UTuentiW,UnofficialTuentiWidget:onReceive", "kernel update");
                this.onUpdate(context, AppWidgetManager.getInstance(context), new int[]{widgetId});
            }else if(action.equals(AppWidgetManager.EXTRA_APPWIDGET_ID)){
                if (LOGGING) Log.d("UTuentiW,UnofficialTuentiWidget:onReceive", "other update ?¿");
                this.onUpdate(context, AppWidgetManager.getInstance(context), new int[]{widgetId});
            }
        }
        else
            super.onReceive(context, intent);
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,int appWidgetId, boolean onlyResized) {

        if (LOGGING) Log.d("UTuentiW,UnofficialTuentiWidget:updateAppWidget ","begin");
        HashMap<String, String> dataMap = UnofficialTuentiWidgetConfigureActivity.loadData(context, appWidgetId);
        //Extract widget size
        int dp = 0;
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
            int maxWidth = appWidgetManager.getAppWidgetOptions(appWidgetId).getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH);
            //int minHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT);
            int maxHeight = appWidgetManager.getAppWidgetOptions(appWidgetId).getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT);
            dp = maxWidth < maxHeight ? maxWidth : maxHeight;
            //first call (while configuring)
            if (dp == 0)
                dp = 40;
        }else{
            if (LOGGING) Log.d("UTuentiW,UnofficialTuentiWidget:updateAppWidget", "xdpi =" + context.getResources().getDisplayMetrics().xdpi);
            WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            Display display = wm.getDefaultDisplay();
            int width = 0;
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB_MR2) {
                width = display.getWidth();  // deprecated
            }else{
                Point size = new Point();
                display.getSize(size);
                width = size.x;
            }
            dp = width/4;
        }
        // Construct the RemoteViews object
        squareSide = Math.round(dp * (context.getResources().getDisplayMetrics().xdpi / DisplayMetrics.DENSITY_DEFAULT));
        if (LOGGING) Log.d("UTuentiW,UnofficialTuentiWidget:updateAppWidget", "dp = " + dp + ", squareSide = " + squareSide);
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.unofficial_tuenti_widget);
        int orientation = context.getResources().getConfiguration().orientation;
        boolean landscape = orientation == Configuration.ORIENTATION_LANDSCAPE ? true : false;
        NetworkTask nt = new NetworkTask(context,views,appWidgetManager,appWidgetId, squareSide, landscape);
        if(onlyResized){
            if (LOGGING) Log.d("UTuentiW,UnofficialTuentiWidget:updateAppWidget", "Just rezised (rotation)");
            String result[] = {"", "", "", "", ""};
            result[0] = dataMap.get("dataMoney");
            result[1] = dataMap.get("dataNet");
            result[2] = dataMap.get("dataPercentage");
            result[3] = dataMap.get("dataVoiceNet");
            result[4] = dataMap.get("dataVoicePercentage");
            nt.updateRemoteViews(result);
        }else {
            nt.execute(dataMap);
        }
    }

    protected void cancelAlarmManager(Context context, int widgetID)
    {
        if (LOGGING) Log.d("UTuentiW,UnofficialTuentiWidget:cancelAlarmManager ","begin");
        AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intentUpdate = new Intent(context, UnofficialTuentiWidget.class);
        //AlarmManager are identified with Intent's Action and Uri.
        intentUpdate.setAction(UPDATE_WIDGET);
        //For a global AlarmManager, don't put the uri to cancel
        //all the AlarmManager with action UPDATE_ONE.
        Uri.Builder build = new Uri.Builder();
        build.appendPath(""+widgetID);
        Uri uri = build.build();
        intentUpdate.setData(uri);
        intentUpdate.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetID);
        PendingIntent pendingIntentAlarm = PendingIntent.getBroadcast(context,
                widgetID,
                intentUpdate,
                PendingIntent.FLAG_UPDATE_CURRENT);

        alarm.cancel(pendingIntentAlarm);
        if (LOGGING) Log.d("UTuentiW,cancelAlarmManager", "Cancelled Alarm. Action = " +
                UnofficialTuentiWidget.UPDATE_WIDGET +
                " URI = " + uri.toString());
    }
}


