/**
 * Created by Enrique García Orive on 21/05/14.
 */

package com.whatafabric.unofficialtuentiwidget;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.RemoteViews;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;


/**
 * The configuration screen for the {@link UnofficialTuentiWidget UnofficialTuentiWidget} AppWidget.
 */
public class UnofficialTuentiWidgetConfigureActivity extends Activity {

    int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    EditText tuUserText;
    EditText tuPasswordText;
    EditText tuBundlePriceText;
    EditText tuVATText;
    private int seconds = 3600;
    protected static final String FILENAME = "UnoficialTuentiData";

    public UnofficialTuentiWidgetConfigureActivity() {
        super();
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        Log.d("UTuentiW,UnofficialTuentiWidgetConfigureActivity:onCreate ", "begin");

        // Set the result to CANCELED.  This will cause the widget host to cancel
        // out of the widget placement if the user presses the back button.
        setResult(RESULT_CANCELED);

        setContentView(R.layout.unofficial_tuenti_widget_configure);
        tuUserText = (EditText)findViewById(R.id.tu_user);
        tuPasswordText = (EditText)findViewById(R.id.tu_password);
        tuBundlePriceText = (EditText)findViewById(R.id.tu_bundlePrice);
        tuVATText = (EditText)findViewById(R.id.tu_vat);
        findViewById(R.id.create_tuentiwidget).setOnClickListener(mOnClickListener);

        // Find the widget id from the intent.
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            mAppWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        // If this activity was started with an intent without an app widget ID, finish with an error.
        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
            return;
        }

        Log.d("UTuentiW,UnofficialTuentiWidgetConfigureActivity:onCreate ", "mAppWidgetId = " + mAppWidgetId);
        tuUserText.setText("user@email.com");
        tuPasswordText.setText("password");
        tuBundlePriceText.setText("0");
        tuVATText.setText("0.21");
        tuPasswordText.requestFocus();


    }

    View.OnClickListener mOnClickListener = new View.OnClickListener() {
        public void onClick(View v) {
            final Context context = UnofficialTuentiWidgetConfigureActivity.this;
            Log.d("UTuentiW,UnofficialTuentiWidgetConfigureActivity:mOnClickListener ", "begin");
            Log.d("UTuentiW,UnofficialTuentiWidgetConfigureActivity:mOnClickListener ","mAppWidgetId = " + mAppWidgetId);

            // When the button is clicked, store the string locally
            String widgetTuUserText = tuUserText.getText().toString();
            String widgetTuPasswordText = tuPasswordText.getText().toString();
            String widgetTuBundlePriceText = tuBundlePriceText.getText().toString();
            String widgetTuVATText = tuVATText.getText().toString();

            HashMap<String, String> dataMap = UnofficialTuentiWidgetConfigureActivity.loadData(context, mAppWidgetId);

            dataMap.put(mAppWidgetId + "_user",widgetTuUserText);
            dataMap.put(mAppWidgetId + "_password",widgetTuPasswordText);
            dataMap.put(mAppWidgetId + "_dataMoney","0 €");
            dataMap.put(mAppWidgetId + "_dataNet","");
            dataMap.put(mAppWidgetId + "_dataPercentage","100");
            dataMap.put(mAppWidgetId + "_dataBundlePrice",widgetTuBundlePriceText);
            dataMap.put(mAppWidgetId + "_dataVAT",widgetTuVATText);

            saveData(context, dataMap);


            // It is the responsibility of the configuration activity to update the app widget
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            UnofficialTuentiWidget.updateAppWidget(context, appWidgetManager, mAppWidgetId, false);

            // Make sure we pass back the original appWidgetId
            Intent resultValue = new Intent();
            resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);


            // New intent for AlarmManager
            Uri.Builder build = new Uri.Builder();
            build.appendPath(""+mAppWidgetId);
            Uri uri = build.build();
            Intent intentUpdate = new Intent(context, UnofficialTuentiWidget.class);
            intentUpdate.setAction(UnofficialTuentiWidget.UPDATE_WIDGET);//Set an action anyway to filter it in onReceive()
            intentUpdate.setData(uri);//One Alarm per instance.
            //We will need the exact instance to identify the intent.
            UnofficialTuentiWidget.addUri(mAppWidgetId, uri);
            intentUpdate.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
            PendingIntent pendingIntentAlarm = PendingIntent.getBroadcast(UnofficialTuentiWidgetConfigureActivity.this,
                    mAppWidgetId,
                    intentUpdate,
                    PendingIntent.FLAG_UPDATE_CURRENT);

            //Custom alarm that will update only when the system lets us do it.
            AlarmManager alarmManager = (AlarmManager)getSystemService(ALARM_SERVICE);
            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP,
                    System.currentTimeMillis()+(seconds*1000),
                    (seconds*1000),
                    pendingIntentAlarm);
            Log.d("UTuentiW,Ok Button", "Created Alarm. Action = " + UnofficialTuentiWidget.UPDATE_WIDGET +
                    " URI = " + build.build().toString() +
                    " Seconds = " + seconds);

            //Create another intent for the case in which we push the widget
            Intent intentForceUpdate = new Intent(context, UnofficialTuentiWidget.class);
            intentForceUpdate.setAction(UnofficialTuentiWidget.FORCE_UPDATE_WIDGET);
            intentForceUpdate.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
            PendingIntent pendingIntentForceUpdate = PendingIntent.getBroadcast(context,
                    mAppWidgetId,
                    intentForceUpdate,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.unofficial_tuenti_widget);
            views.setOnClickPendingIntent(R.id.dataMoney,pendingIntentForceUpdate);
            views.setOnClickPendingIntent(R.id.dataNet,pendingIntentForceUpdate);

            appWidgetManager.updateAppWidget(mAppWidgetId, views);

            setResult(RESULT_OK, resultValue);
            finish();
        }
    };

    //Save data object needed by the widget in a private file
    static void saveData(Context context, HashMap<String, String> dataMap) {
        Log.d("UTuentiW,UnofficialTuentiWidgetConfigureActivity:saveData ", "begin");
        File file = new File(context.getDir("data", MODE_PRIVATE), FILENAME);
        for (HashMap.Entry<String, String> entry : dataMap.entrySet())
        {
            if(!entry.getKey().contains("password"))
                Log.d("UTuentiW,UnofficialTuentiWidgetConfigureActivity:saveData ", entry.getKey() + "/" + entry.getValue());
        }
        try {
            ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(file));
            outputStream.writeObject(dataMap);
            outputStream.flush();
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Read the object from the private file  for this widget.
    // If there is no file saved, create one with the default values.
    static HashMap<String, String> loadData(Context context, int appWidgetId) {
        Log.d("UTuentiW,UnofficialTuentiWidgetConfigureActivity:loadData ", "begin");
        HashMap<String,String> dataMap = new HashMap<String, String>();
        File file = new File(context.getDir("data", MODE_PRIVATE), FILENAME);
        try {
            if (file.exists()){
                Log.d("UTuentiW,UnofficialTuentiWidgetConfigureActivity:loadData ","file exists.");
                FileInputStream fis = new FileInputStream(file);
                ObjectInputStream ois = new ObjectInputStream(fis);

                dataMap = (HashMap<String, String>) ois.readObject();
                for (HashMap.Entry<String, String> entry : dataMap.entrySet())
                {
                    if(!entry.getKey().contains("password"))
                        Log.d("UTuentiW,UnofficialTuentiWidgetConfigureActivity:loadData ", entry.getKey() + "/" + entry.getValue());
                }
            }else{
                Log.d("UTuentiW,UnofficialTuentiWidgetConfigureActivity:onCreate ","file doesn't exists.");
                dataMap.put(appWidgetId+"_user","user");
                dataMap.put(appWidgetId+"_password","password");
                dataMap.put(appWidgetId+"_dataMoney","0 €");
                dataMap.put(appWidgetId+"_dataNet","0 MB");
                dataMap.put(appWidgetId+"_dataPercentage","0");
                dataMap.put(appWidgetId+"_dataBundlePrice","0");
                dataMap.put(appWidgetId+"_dataVAT","0.21");

                ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(file));
                outputStream.writeObject(dataMap);
                outputStream.flush();
                outputStream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e){
            e.printStackTrace();
        }
        return dataMap;
    }


    //Remove the private file
    static void deleteData(Context context, int appWidgetId) {
        Log.d("UTuentiW,UnofficialTuentiWidgetConfigureActivity:deleteData ", "begin");
        File file = new File(context.getDir("data", MODE_PRIVATE), FILENAME);
        HashMap<String,String> dataMap,dataMapRemoveElements = new HashMap<String, String>();

        if (file.exists()) {
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(file);
                ObjectInputStream ois = new ObjectInputStream(fis);
                dataMap = (HashMap<String, String>) ois.readObject();
                dataMapRemoveElements = (HashMap<String, String>) dataMap.clone();
                if(dataMap instanceof HashMap && dataMap.size()>0) {
                    for (HashMap.Entry<String, String> entry : dataMap.entrySet()) {
                        String key = entry.getKey();
                        if (key.contains(String.valueOf(appWidgetId))) {
                            dataMapRemoveElements.remove(key);
                        }
                    }
                }
                //Save the object again
                ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(file));
                outputStream.writeObject(dataMapRemoveElements);
                outputStream.flush();
                outputStream.close();
                Log.d("UTuentiW,UnofficialTuentiWidgetConfigureActivity:deleteData ", "deleted data of widgetId = " + appWidgetId);


            }catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e){
                e.printStackTrace();
            }

            //If after remove the data corresponding to this widget exists data from others we don't delete the file.
            if (dataMapRemoveElements.size()>0){
                Log.d("UTuentiW,UnofficialTuentiWidgetConfigureActivity:deleteData ", "file still has other data so we do NOT delete it.");
            }else {
                Log.d("UTuentiW,UnofficialTuentiWidgetConfigureActivity:deleteData ", "file exists and no other data remain so lets delete it");
                file.delete();
                Log.d("UTuentiW,UnofficialTuentiWidgetConfigureActivity:deleteData ", "file deleted.");
            }
        }
    }
}



