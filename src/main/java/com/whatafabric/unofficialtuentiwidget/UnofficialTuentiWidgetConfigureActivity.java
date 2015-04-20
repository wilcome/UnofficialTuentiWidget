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
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RemoteViews;
import android.widget.TextView;

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
    private static boolean LOGGING = true;
    int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    EditText tuUserText;
    EditText tuPasswordText;
    TextView tuBundlePriceText;
    TextView tuVATText;
    EditText tuBundlePriceEditText;
    EditText tuVATEditText;
    protected static int seconds = 3600;

    protected static final String FILENAME = "UnoficialTuentiData";

    public UnofficialTuentiWidgetConfigureActivity() {
        super();
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        if (LOGGING) Log.d("ConfigureActivity:onCreate ", "begin");

        // Set the result to CANCELED.  This will cause the widget host to cancel
        // out of the widget placement if the user presses the back button.
        setResult(RESULT_CANCELED);

        setContentView(R.layout.unofficial_tuenti_widget_configure);
        tuUserText = (EditText)findViewById(R.id.tu_user);
        tuPasswordText = (EditText)findViewById(R.id.tu_password);
        tuBundlePriceText = (TextView)findViewById(R.id.tu_bundlePriceText);
        tuVATText = (TextView)findViewById(R.id.tu_vatText);
        tuBundlePriceEditText = (EditText)findViewById(R.id.tu_bundlePriceEditText);
        tuVATEditText = (EditText)findViewById(R.id.tu_vatEditText);
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

        if (LOGGING) Log.d("ConfigureActivity:onCreate ", "mAppWidgetId = " + mAppWidgetId);
        tuUserText.setText("user@email.com");
        tuPasswordText.setText("password");

        tuBundlePriceText.setVisibility(View.GONE);
        tuVATText.setVisibility(View.GONE);
        tuBundlePriceEditText.setText("0");
        tuVATEditText.setText("0.0");
        tuBundlePriceEditText.setVisibility(View.GONE);
        tuVATEditText.setVisibility(View.GONE);
        tuPasswordText.requestFocus();
    }

    public void onCheckboxClicked(View view) {
        boolean checked = ((CheckBox) view).isChecked();
        switch(view.getId()) {
            case R.id.checkbox_postpaid:
                if (checked) {
                    tuBundlePriceText.setVisibility(View.VISIBLE);
                    tuVATText.setVisibility(View.VISIBLE);
                    tuBundlePriceEditText.setText("0");
                    tuVATEditText.setText("0.21");
                    tuBundlePriceEditText.setVisibility(View.VISIBLE);
                    tuVATEditText.setVisibility(View.VISIBLE);
                    break;
                }else {
                    tuBundlePriceText.setVisibility(View.GONE);
                    tuVATText.setVisibility(View.GONE);
                    tuBundlePriceEditText.setText("0");
                    tuVATEditText.setText("0.0");
                    tuBundlePriceEditText.setVisibility(View.GONE);
                    tuVATEditText.setVisibility(View.GONE);
                    break;
                }
        }
    }

    View.OnClickListener mOnClickListener = new View.OnClickListener() {
        public void onClick(View v) {
            final Context context = UnofficialTuentiWidgetConfigureActivity.this;
            if (LOGGING) Log.d("UTuentiW,UnofficialTuentiWidgetConfigureActivity:mOnClickListener ", "begin");
            if (LOGGING) Log.d("UTuentiW,UnofficialTuentiWidgetConfigureActivity:mOnClickListener ","mAppWidgetId = " + mAppWidgetId);

            // When the button is clicked, store the string locally
            String widgetTuUserText = tuUserText.getText().toString();
            String widgetTuPasswordText = tuPasswordText.getText().toString();
            String widgetTuBundlePriceText = tuBundlePriceEditText.getText().toString();
            String widgetTuVATText = tuVATEditText.getText().toString();

            HashMap<String, String> dataMap = UnofficialTuentiWidgetConfigureActivity.loadData(context, mAppWidgetId);

            dataMap.put("user",widgetTuUserText);
            dataMap.put("password",widgetTuPasswordText);
            dataMap.put("dataMoney","0 €");
            dataMap.put("dataNet","");
            dataMap.put("dataPercentage","100");
            dataMap.put("dataVoiceNet","");
            dataMap.put("dataVoicePercentage","100");
            dataMap.put("dataDays","");
            dataMap.put("dataDaysPercentage","100");
            dataMap.put("dataBundlePrice",widgetTuBundlePriceText);
            dataMap.put("dataVAT",widgetTuVATText);

            saveData(context, dataMap, mAppWidgetId);

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
            intentUpdate.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
            PendingIntent pendingIntentAlarm = PendingIntent.getBroadcast(UnofficialTuentiWidgetConfigureActivity.this,
                    mAppWidgetId,
                    intentUpdate,
                    PendingIntent.FLAG_UPDATE_CURRENT);

            //Custom alarm that will update only when the system lets us do it.
            AlarmManager alarmManager = (AlarmManager)getSystemService(ALARM_SERVICE);
            alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime()+(seconds*1000),
                    (seconds*1000),
                    pendingIntentAlarm);
            if (LOGGING) Log.d("UTuentiW,UnofficialTuentiWidgetConfigureActivity:mOnClickListener",
                    "Created Alarm. Action = " + UnofficialTuentiWidget.UPDATE_WIDGET +
                    " URI = " + uri.toString() +
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
            views.setOnClickPendingIntent(R.id.dataVoice,pendingIntentForceUpdate);


            appWidgetManager.updateAppWidget(mAppWidgetId, views);

            setResult(RESULT_OK, resultValue);
            finish();
        }
    };

    //Save data object needed by the widget in a private file
    static void saveData(Context context, HashMap<String, String> dataMap, int appWidgetId) {
        if (LOGGING) Log.d("UTuentiW,UnofficialTuentiWidgetConfigureActivity:saveData ", "begin");
        //File file = new File(context.getDir("data", MODE_PRIVATE), FILENAME + "_" + appWidgetId);

        File file = new File(context.getFilesDir(), FILENAME + "_" + appWidgetId);


        //If file doesn't already exist we don't do anything.
        if (file.exists()) {
            HashMap<String, String> internalDataMap = new HashMap<String, String>();

            for (HashMap.Entry<String, String> entry : dataMap.entrySet()) {
                    internalDataMap.put(entry.getKey(), entry.getValue());
                if (!entry.getKey().contains("password"))
                    if (LOGGING)
                        Log.d("UTuentiW,UnofficialTuentiWidgetConfigureActivity:saveData ", entry.getKey() + "/" + entry.getValue());
            }
            try {
                ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(file));
                outputStream.writeObject(internalDataMap);
                outputStream.flush();
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Read the object from the private file  for this widget.
    // If there is no file saved, create one with the default values.
    static HashMap<String, String> loadData(Context context, int appWidgetId) {
        if (LOGGING) Log.d("UTuentiW,UnofficialTuentiWidgetConfigureActivity:loadData ", "begin");

        //File oldFile = new File(context.getDir("data", MODE_PRIVATE), FILENAME);

        File oldFile = new File(context.getFilesDir(), FILENAME);


        if (oldFile.exists()){
            try {
                HashMap<String, String> oldDataMap;
                if (LOGGING) Log.d("UTuentiW,UnofficialTuentiWidgetConfigureActivity:loadData ", "old file exists.");
                FileInputStream oldFis = new FileInputStream(oldFile);
                ObjectInputStream oldOis = new ObjectInputStream(oldFis);
                if (LOGGING) Log.d("UTuentiW,UnofficialTuentiWidgetConfigureActivity:loadData ", "after streams");

                oldDataMap = (HashMap<String, String>) oldOis.readObject();
                HashMap<String, String> internalDataMap = new HashMap<String, String>();
                HashMap<String, String> newDataMap = new HashMap<String, String>();
                for (HashMap.Entry<String, String> entry : oldDataMap.entrySet()) {
                    if (entry.getKey().contains(Integer.toString(appWidgetId)+"_")) {
                        if (!entry.getKey().contains("password"))
                            if (LOGGING) Log.d("UTuentiW,UnofficialTuentiWidgetConfigureActivity:loadData ", entry.getKey() + "/" + entry.getValue());

                        if (LOGGING) Log.d("UTuentiW,UnofficialTuentiWidgetConfigureActivity:loadData ", "trying to extract value!");
                        String key = entry.getKey().substring(entry.getKey().indexOf('_')+1);
                        if (LOGGING) Log.d("UTuentiW,UnofficialTuentiWidgetConfigureActivity:loadData ", "key=" + key + " ,value=" + entry.getValue());
                        newDataMap.put(key, entry.getValue());
                    } else {
                        internalDataMap.put(entry.getKey(),entry.getValue());
                        if (LOGGING) Log.d("UnofficialTuentiWidgetConfigureActivity:saveData BUG UNSOLVED: ", entry.getKey() + "/" + entry.getValue());
                    }
                }


                //check if there is still data or not. If not, remove the old file
                if(oldDataMap.isEmpty()) {
                    if (LOGGING) Log.d("UTuentiW,UnofficialTuentiWidgetConfigureActivity:loadData ", "OldMap empty so remove oldFile.");
                    oldFile.delete();
                }else{
                    //Save oldMap without the items extracted
                    try {
                        if (LOGGING) Log.d("UTuentiW,UnofficialTuentiWidgetConfigureActivity:loadData ", "Save oldMap with the rest of the data.");
                        ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(oldFile));
                        outputStream.writeObject(internalDataMap);
                        outputStream.flush();
                        outputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                if (LOGGING) Log.d("UTuentiW,UnofficialTuentiWidgetConfigureActivity:loadData ", "Trying to create new file.");
                //File newFile = new File(context.getDir("data", MODE_PRIVATE), FILENAME + "_" + appWidgetId);
                File newFile = new File(context.getFilesDir(), FILENAME + "_" + appWidgetId);

                if(!newFile.exists()) {
                    try {
                    ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(newFile));
                    outputStream.writeObject(newDataMap);
                    outputStream.flush();
                    outputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }


        HashMap<String,String> dataMap = new HashMap<String, String>();
        //File file = new File(context.getDir("data", MODE_PRIVATE), FILENAME + "_" + appWidgetId);
        File file = new File(context.getFilesDir(), FILENAME + "_" + appWidgetId);

        try {
            if (file.exists()){
                if (LOGGING) Log.d("UTuentiW,UnofficialTuentiWidgetConfigureActivity:loadData ","New file format found.");
                FileInputStream fis = new FileInputStream(file);
                ObjectInputStream ois = new ObjectInputStream(fis);

                dataMap = (HashMap<String, String>) ois.readObject();
                for (HashMap.Entry<String, String> entry : dataMap.entrySet())
                {
                    if(!entry.getKey().contains("password"))
                        if (LOGGING) Log.d("UTuentiW,UnofficialTuentiWidgetConfigureActivity:loadData ", entry.getKey() + "/" + entry.getValue());
                }
            }else{
                if (LOGGING) Log.d("UTuentiW,UnofficialTuentiWidgetConfigureActivity:onCreate ","file doesn't exists.");
                dataMap.put("user","user");
                dataMap.put("password","password");
                dataMap.put("dataMoney","0 €");
                dataMap.put("dataNet","0 MB");
                dataMap.put("dataPercentage","0");
                dataMap.put("dataVoiceNet","0m");
                dataMap.put("dataVoicePercentage","0");
                dataMap.put("dataDays","0d");
                dataMap.put("dataDaysPercentage","0");
                dataMap.put("dataBundlePrice","0");
                dataMap.put("dataVAT","0.21");

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
        if (LOGGING) Log.d("UTuentiW,UnofficialTuentiWidgetConfigureActivity:deleteData ", "begin");
        String filename = FILENAME + "_" + appWidgetId;
        //File file = new File(context.getDir("data", MODE_PRIVATE), filename);
        File file = new File(context.getFilesDir(), filename);

        if (file.exists()) {
            file.delete();
            if (LOGGING) Log.d("UTuentiW,UnofficialTuentiWidgetConfigureActivity:deleteData ", "file " + filename + " deleted.");
        }
    }
}



