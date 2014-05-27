package com.whatafabric.unofficialtuentiwidget;

/**
 * Created by Enrique García Orive on 22/05/14.
 */

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.RemoteViews;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import javax.net.ssl.HttpsURLConnection;


public class NetworkTask extends AsyncTask<HashMap<String,String>, Void, String[]> {

    private static int SLEEPING_TIME = 1000; //in miliseconds
    private static int COUNT_LIMIT = 5; //in miliseconds

    private Context context;
    private RemoteViews remoteViews;
    private AppWidgetManager appWidgetManager;
    int appWidgetId;


    public NetworkTask(Context context,
                       RemoteViews remoteViews,
                       AppWidgetManager appWidgetManager,
                       int appWidgetId){
        Log.d("NetworkTask:NetworkTask ", "Starts");

        this.remoteViews = remoteViews;
        this.appWidgetManager = appWidgetManager;
        this.appWidgetId = appWidgetId;
        this.context = context;
    }


    @Override
    protected String[] doInBackground(HashMap<String,String> ... params) {
        Log.d("NetworkTask:doInBackground ", "Starts");

        CookieManager cookieManager = new CookieManager();
        CookieHandler.setDefault(cookieManager);
        HashMap<String,String> dataMap = params[0];
        String user = dataMap.get(appWidgetId + "_user");
        String password = dataMap.get(appWidgetId + "_password");
        String dataMoney = dataMap.get(appWidgetId + "_dataMoney");
        String dataNet = dataMap.get(appWidgetId + "_dataNet");
        String dataPercentage = dataMap.get(appWidgetId + "_dataPercentage");
        String dataBundlePrice = dataMap.get(appWidgetId + "_dataBundlePrice");

        //Log.d("NetworkTask ", "user = " + user);
        //Log.d("NetworkTask ", "password = " + password);
        Log.d("NetworkTask:doInBackground ", "OLD dataMoney = " + dataMoney);
        Log.d("NetworkTask:doInBackground ", "OLD dataNet = " + dataNet);
        Log.d("NetworkTask:doInBackground ", "OLD dataPercentage = " + dataPercentage);
        Log.d("NetworkTask:doInBackground ", "OLD dataBundlePrice = " + dataBundlePrice);

        String result[] = {"","",""};

        result[0] = dataMoney;
        result[1] = dataNet;
        result[2] = dataPercentage;


        if(user != null && password != null) {

            URL url;

            try {
                String link = "https://www.tuenti.com/?m=Login";
                url = new URL(link);
                HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
                urlConnection.setRequestProperty("Referer", "https://www.tuenti.com/?gotHash=1");
                urlConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (X11; Ubuntu; Linux i686; rv:29.0) Gecko/20100101 Firefox/29.0");
                urlConnection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");

                InputStream in = urlConnection.getInputStream();
                InputStreamReader isr = new InputStreamReader(in);

                String sreturned = "";
                char[] buf = new char[16384];
                int read;
                StringBuffer sb = new StringBuffer();
                while ((read = isr.read(buf)) > 0) {
                    sb.append(buf, 0, read);
                }
                sreturned = sb.toString();

                //Obtain CSRF from body
                String csrf = "";
                int csrfStartIndex = -1;
                if (sreturned.indexOf("csrf") != 1) {
                    csrfStartIndex = sreturned.indexOf("csrf") + 13;
                } else if (sreturned.indexOf("csfr") != 1) {
                    csrfStartIndex = sreturned.indexOf("csfr") + 13;
                } else {
                    Log.d("NetworkTask:doInBackground ","CSRF NOT found");
                    return result;
                }
                int csrfEndIndex = sreturned.indexOf("\"", csrfStartIndex);
                csrf = sreturned.substring(csrfStartIndex, csrfEndIndex);
                Log.d("NetworkTask:doInBackground ","CSRF = " + csrf);


                //Login with user & password
                link = "https://secure.tuenti.com/?m=Login&func=do_login";

                url = new URL(link);
                urlConnection = (HttpsURLConnection) url.openConnection();
                urlConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (X11; Ubuntu; Linux i686; rv:29.0) Gecko/20100101 Firefox/29.0");
                urlConnection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
                urlConnection.setRequestProperty("Referer", "https://www.tuenti.com/?m=Login");
                urlConnection.setRequestMethod("POST");
                urlConnection.setDoInput(true);
                urlConnection.setDoOutput(true);

                String userEncoded = URLEncoder.encode(user, "UTF-8");
                String passwordEncoded = URLEncoder.encode(password, "UTF-8");
                //Log.d("NetworkTask ", "userEncoded = " + userEncoded);
                //Log.d("NetworkTask ", "passwordEncoded = " + passwordEncoded);
                String urlParameters = "timezone=1&timestamp=1&email=" + userEncoded + "&input_password=" + passwordEncoded + "&csfr=" + csrf;
                DataOutputStream wr = new DataOutputStream(urlConnection.getOutputStream());
                wr.writeBytes(urlParameters);
                wr.flush();
                wr.close();

                int status = urlConnection.getResponseCode();
                Log.d("NetworkTask:doInBackground ", "status = " + status);

                link = "https://www-1.tuenti.com/?m=Accountdashboard&func=index&utm_content=active_subscriber&utm_source=internal&utm_medium=mobile_tab&utm_campaign=cupcake_fixed&ajax=1";

                url = new URL(link);
                urlConnection = (HttpsURLConnection) url.openConnection();
                urlConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (X11; Ubuntu; Linux i686; rv:29.0) Gecko/20100101 Firefox/29.0");
                urlConnection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
                urlConnection.setRequestProperty("Accept-Language", "es-ES,es;q=0.8,en-US;q=0.5,en;q=0.3");
                urlConnection.setRequestProperty("Accept-Encoding", "gzip, deflate");
                urlConnection.setRequestProperty("Referer", "https://www.tuenti.com/?m=Login");
                urlConnection.setRequestProperty("Connection", "Keep-alive");
                urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                in = urlConnection.getInputStream();

                if ("gzip".equals(urlConnection.getContentEncoding())) {
                    in = new GZIPInputStream(in);
                }

                isr = new InputStreamReader(in);
                StringBuffer sb2 = new StringBuffer();
                while ((read = isr.read(buf)) > 0) {
                    sb2.append(buf, 0, read);
                }

                int counter = 0;
                int sleepingTime = SLEEPING_TIME;
                while(counter!=COUNT_LIMIT){
                    Log.d("NetworkTask:doInBackground ", "Sleeping " + SLEEPING_TIME + " s" );
                    Thread.sleep(sleepingTime); //Wait data for being accessible and then request again
                    Log.d("NetworkTask:doInBackground ", "Time to wake up and ask again!");
                    url = new URL(link);
                    urlConnection = (HttpsURLConnection) url.openConnection();
                    urlConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (X11; Ubuntu; Linux i686; rv:29.0) Gecko/20100101 Firefox/29.0");
                    urlConnection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
                    urlConnection.setRequestProperty("Accept-Language", "es-ES,es;q=0.8,en-US;q=0.5,en;q=0.3");
                    urlConnection.setRequestProperty("Accept-Encoding", "gzip, deflate");
                    urlConnection.setRequestProperty("Referer", "https://www.tuenti.com/?m=Login");
                    urlConnection.setRequestProperty("Connection", "Keep-alive");
                    urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    in = urlConnection.getInputStream();

                    if ("gzip".equals(urlConnection.getContentEncoding())) {
                        in = new GZIPInputStream(in);
                    }

                    isr = new InputStreamReader(in);
                    sb2 = new StringBuffer();
                    while ((read = isr.read(buf)) > 0) {
                        sb2.append(buf, 0, read);
                    }
                    sreturned = sb2.toString();

                    //StoreResponse(sreturned, counter);

                    if((sreturned.contains("tu-dash-balance")) &&
                            !(sreturned.contains(context.getResources().getString(R.string.loading) )))
                        break;
                    counter++;
                    sleepingTime += sleepingTime;
                }

                if(counter==COUNT_LIMIT) {
                    Log.d("NetworkTask:doInBackground ", "data not found. :'(");
                    return result;
                }

                Log.d("NetworkTask:doInBackground ", "Some data found! :)");
                String consumption = "";
                Pattern pc1 = Pattern.compile("\\\\u20ac\\\\u00a0([0-9]+?\\.[0-9]*)");//Cases:  €0.15
                Matcher mc1 = pc1.matcher(sreturned);
                Pattern pc2 = Pattern.compile("\\\\u20ac\\\\u00a0([0-9]+?)");//Cases:  €0 or  €4
                Matcher mc2 = pc2.matcher(sreturned);
                Pattern pc3 = Pattern.compile("([0-9]+?,[0-9]*)[^,]*\\\\u20ac");//Cases:  0,15€
                Matcher mc3 = pc3.matcher(sreturned);
                Pattern pc4 = Pattern.compile("([0-9]++)[^,0-9]*\\\\u20ac");//Cases:  0€ or 4€
                Matcher mc4 = pc4.matcher(sreturned);

                if (mc1.find()){
                    Log.d("NetworkTask:doInBackground ", "group1 = " + mc1.group(1));
                    consumption = mc1.group(1);
                }else if (mc2.find()){
                    Log.d("NetworkTask:doInBackground ", "group2 = " + mc2.group(1));
                    consumption = mc2.group(1);
                }else if (mc3.find()){
                    Log.d("NetworkTask:doInBackground ", "group3 = " + mc3.group(1));
                    consumption = mc3.group(1);
                }else if (mc4.find()) {
                    Log.d("NetworkTask:doInBackground ", "group4 = " + mc4.group(1));
                    consumption = mc4.group(1);
                }else{
                    //No money
                    result[0]="badAccount";
                }

                consumption = consumption.replace(',','.');
                double consumptionDouble = (double) Math.round((Double.parseDouble(consumption) +
                        Double.parseDouble(dataBundlePrice))*100)/100;
                Log.d("NetworkTask:doInBackground ", consumptionDouble + " €");
                result[0] = consumptionDouble + " €";


                String percentage = "";
                Pattern patternPER = Pattern.compile("percentage\\\":(\\d+).?");
                Matcher matcherPER = patternPER.matcher(sreturned);
                if (matcherPER.find()) {
                    percentage = matcherPER.group(1);
                    result[2]=percentage;
                    Log.d("NetworkTask ", percentage + " %");

                    //percentage found ... It should exists bundle data
                    Pattern patternMB = Pattern.compile("([0-9]{1,3}+)[^0-9]*>MB ");
                    Pattern patternGB = Pattern.compile("([0-9]{1,2}+)\\.([0-9]{3}+)[^0-9]*>MB ");
                    Matcher matcherMB = patternMB.matcher(sreturned);
                    Matcher matcherGB = patternGB.matcher(sreturned);
                    if (matcherGB.find()) {
                        String megaBytes = matcherGB.group(1)+matcherGB.group(2);
                        double gigasDouble = (double)Math.round((Double.parseDouble(megaBytes) / 1024) * 100) / 100;
                        Log.d("NetworkTask ",  + gigasDouble + " GB");
                        result[1] = gigasDouble + " GB";
                    }else if (matcherMB.find()) {
                        result[1] = matcherMB.group(1) + " MB";
                    }else{
                        //No bundle
                        result[1]="";
                    }

                }

            } catch (MalformedURLException e) {
                Log.e("Network,doInBackground: ","Error MalformedURLException");
                e.printStackTrace();
                return result;
            } catch (InterruptedException e) {
                Log.e("Network,doInBackground: ","Error InterruptedException");
                e.printStackTrace();
                return result;
            } catch (IOException e) {
                Log.e("Network,doInBackground: ","Error IOException");
                e.printStackTrace();
                return result;
            }
        }

        //Before finish store the results in the Map
        dataMap.put(appWidgetId + "_dataMoney",result[0]);
        dataMap.put(appWidgetId + "_dataNet",result[1]);
        dataMap.put(appWidgetId + "_dataPercentage",result[2]);
        UnofficialTuentiWidgetConfigureActivity.saveData(context, dataMap);

        return result;
    }

    @Override
    protected void onPostExecute(String[] result) {
        if(result != null){
            Log.d("NetworkTask, onPostExecute ","result[0]" + result[0]);
            Log.d("NetworkTask, onPostExecute ","result[1]" + result[1]);
            Log.d("NetworkTask, onPostExecute ","result[2]" + result[2]);

            remoteViews.setTextViewText(R.id.dataMoney, result[0]);
            if(result[1]!="" && result[1]!=null) {
                remoteViews.setTextViewText(R.id.dataNet, result[1]);
            }else{
                remoteViews.setTextViewText(R.id.dataNet, context.getString(R.string.nobundle));
            }

            int bgId = 0;
            if(result[2]!="" && result[2]!=null){
                bgId = context.getResources().getIdentifier("tuenti_widget_"+result[2]+"_annulus",
                        "drawable",
                        context.getPackageName());

            }else{
                bgId = context.getResources().getIdentifier("tuenti_widget_100_annulus",
                        "drawable",
                        context.getPackageName());
            }
            remoteViews.setImageViewResource(R.id.annulus,bgId);

            //Create another intent for the case in which we push the widget
            Intent intentForceUpdate = new Intent(context, UnofficialTuentiWidget.class);
            intentForceUpdate.setAction(UnofficialTuentiWidget.FORCE_UPDATE_WIDGET);
            intentForceUpdate.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            PendingIntent pendingIntentForceUpdate = PendingIntent.getBroadcast(context,
                    appWidgetId,
                    intentForceUpdate,
                    PendingIntent.FLAG_UPDATE_CURRENT);

            remoteViews.setOnClickPendingIntent(R.id.dataMoney,pendingIntentForceUpdate);
            remoteViews.setOnClickPendingIntent(R.id.dataNet,pendingIntentForceUpdate);


            // Instruct the widget manager to update the widget
            appWidgetManager.updateAppWidget(appWidgetId, remoteViews);

        }else{
            Log.d("NetworkTask, onPostExecute ","Text empty");
        }
    }

    public void StoreResponse(String response, int counter){
        String finalString = "";
        StringBuffer fileData = new StringBuffer();
        File file = new File(context.getDir("data", context.MODE_PRIVATE), "responses.txt");
        BufferedReader reader = null;
        try {
            if(file.exists()) {
                reader = new BufferedReader(new FileReader(file));
                char[] buf = new char[1024];
                int numRead = 0;
                while ((numRead = reader.read(buf)) != -1) {
                    String readData = String.valueOf(buf, 0, numRead);
                    fileData.append(readData);
                }
                reader.close();
            }
            finalString = fileData.toString() + "\n\ncounter ----> " + counter + "\n\n" +  response;

            file = new File(context.getDir("data", context.MODE_PRIVATE), "responses.txt");
            PrintWriter out = new PrintWriter(file);
            out.println(finalString);
            out.flush();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }
}
