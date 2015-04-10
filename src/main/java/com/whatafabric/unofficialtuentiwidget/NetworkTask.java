package com.whatafabric.unofficialtuentiwidget;

/**
 * Created by Enrique García Orive on 22/05/14.
 */

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.RemoteViews;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.HttpsURLConnection;


public class NetworkTask extends AsyncTask<HashMap<String,String>, Void, String[]> {
    private static boolean LOGGING = false;
    private static final String TAG = "NetworkTask";
    private static int SLEEPING_TIME = 1000; //in miliseconds
    private static int COUNT_LIMIT = 5; //in miliseconds

    private Context context;
    private RemoteViews remoteViews;
    private AppWidgetManager appWidgetManager;
    private String dataVAT;
    private String dataBundlePrice;
    int appWidgetId;
    private static int squareSide;
    private static boolean landscape;
    

    public NetworkTask(Context context,
                       RemoteViews remoteViews,
                       AppWidgetManager appWidgetManager,
                       int appWidgetId,
                       int squareSide,
                       boolean landscape){
        if (LOGGING) Log.d(TAG, "Begin");

        this.context = context;
        this.remoteViews = remoteViews;
        this.appWidgetManager = appWidgetManager;
        if (LOGGING) Log.d(TAG, "appWidgetId = " + appWidgetId + ", squareSide = " + squareSide);
        this.appWidgetId = appWidgetId;
        this.squareSide = squareSide;
        this.landscape = landscape;
    }

    @Override
    protected void onPreExecute() {
        if (LOGGING) Log.d(TAG,"Begin");
        remoteViews.setViewVisibility(R.id.ProgressBarLayout, View.VISIBLE);
        if (LOGGING) Log.d(TAG,"ProgressBar VISIBLE, appWidgetd = " + appWidgetId);
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
    }

    @Override
    protected String[] doInBackground(HashMap<String,String> ... params) {
        if (LOGGING) Log.d(TAG, "Begin");

        String result[] = {"", "", "", "", "", "", ""};

        HashMap<String, String> dataMap = params[0];
        String user = dataMap.get("user");
        String password = dataMap.get("password");
        String dataMoney = dataMap.get("dataMoney");
        String dataNet = dataMap.get("dataNet");
        String dataPercentage = dataMap.get("dataPercentage");
        String dataVoiceNet = dataMap.get("dataVoiceNet");
        String dataVoicePercentage = dataMap.get("dataVoicePercentage");
        String dataDays = dataMap.get("dataDays");
        String dataDaysPercentage = dataMap.get("dataDaysPercentage");

        dataBundlePrice = dataMap.get("dataBundlePrice");
        dataVAT = dataMap.get("dataVAT");

        if (LOGGING) Log.d(TAG, "OLD dataMoney = " + dataMoney);
        if (LOGGING) Log.d(TAG, "OLD dataNet = " + dataNet);
        if (LOGGING) Log.d(TAG, "OLD dataPercentage = " + dataPercentage);
        if (LOGGING) Log.d(TAG, "OLD dataBundlePrice = " + dataBundlePrice);
        if (LOGGING) Log.d(TAG, "OLD dataVAT = " + dataVAT);

        result[0] = dataMoney;
        result[1] = dataNet;
        result[2] = dataPercentage;
        result[3] = dataVoiceNet;
        result[4] = dataVoicePercentage;
        result[5] = dataDays;
        result[6] = dataDaysPercentage;

        boolean test = false;
        if(!test){

            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.FROYO) {
                CookieManager cookieManager = new CookieManager();
                CookieHandler.setDefault(cookieManager);

                if (user != null && password != null) {
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
                        Pattern pcsrf = Pattern.compile("\"csrf\"*+[^\"]*+\"([^\"]+)\"|\"csfr\"*+[^\"]*+\"([^\"]+)\"");
                        Matcher mcsrf = pcsrf.matcher(sreturned);

                        if (mcsrf.find()) {
                            if (mcsrf.group(1) != null) {
                                csrf = mcsrf.group(1);
                            } else {
                                csrf = mcsrf.group(2);
                            }
                        }

                        if (LOGGING) Log.d(TAG, "CSRF = " + csrf);

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
                        String urlParameters = "timezone=1&timestamp=1&email=" + userEncoded + "&input_password=" + passwordEncoded + "&csfr=" + csrf;
                        DataOutputStream wr = new DataOutputStream(urlConnection.getOutputStream());
                        wr.writeBytes(urlParameters);
                        wr.flush();
                        wr.close();

                        int status = urlConnection.getResponseCode();
                        if (LOGGING) Log.d(TAG, "status = " + status);

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
                        while (counter != COUNT_LIMIT) {
                            if (LOGGING) Log.d(TAG, "Sleeping " + sleepingTime + " s");
                            Thread.sleep(sleepingTime); //Wait data for being accessible and then request again
                            if (LOGGING) Log.d(TAG, "Time to wake up and ask again!");
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

                            in.close();
                            sreturned = sb2.toString();

                            //StoreResponse(sreturned, counter);

                            if (sreturned.contains("account-page-content"))
                                break;
                            counter++;
                            sleepingTime += sleepingTime;
                        }

                        if (counter == COUNT_LIMIT) {
                            if (LOGGING) Log.d(TAG, "data not found. :'(");
                            return result;
                        }
                        result = extractResult(sreturned);

                    } catch (MalformedURLException e) {
                        if (LOGGING) Log.e(TAG, "Error MalformedURLException");
                        e.printStackTrace();
                        return result;
                    } catch (InterruptedException e) {
                        if (LOGGING) Log.e(TAG, "Error InterruptedException");
                        e.printStackTrace();
                        return result;
                    } catch (IOException e) {
                        if (LOGGING) Log.e(TAG, "Error IOException");
                        e.printStackTrace();
                        return result;
                    }
                }

                //Before finish store the results in the Map
                dataMap.put("dataMoney", result[0]);
                dataMap.put("dataNet", result[1]);
                dataMap.put("dataPercentage", result[2]);
                dataMap.put("dataVoiceNet", result[3]);
                dataMap.put("dataVoicePercentage", result[4]);
                dataMap.put("dataDays", result[5]);
                dataMap.put("dataDaysPercentage", result[6]);
                UnofficialTuentiWidgetConfigureActivity.saveData(context, dataMap, appWidgetId);

                return result;
            }else {
                try {
                    // Create a local instance of cookie store
                    BasicCookieStore cookieStore =  new BasicCookieStore();

                    // Create local HTTP context
                    HttpContext localContext = new BasicHttpContext();
                    // Bind custom cookie store to the local context
                    localContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);

                    HttpGet httpGet = new HttpGet("https://www.tuenti.com/?m=Login");
                    HttpParams httpParameters = new BasicHttpParams();
                    // Set the timeout in milliseconds until a connection is established.
                    // The default value is zero, that means the timeout is not used.
                    int timeoutConnection = 3000;
                    HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);
                    // Set the default socket timeout (SO_TIMEOUT)
                    // in milliseconds which is the timeout for waiting for data.
                    int timeoutSocket = 5000;
                    HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);

                    httpGet.setHeader("Referer", "https://www.tuenti.com/?gotHash=1");
                    httpGet.setHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");

                    AndroidHttpClient httpClient = AndroidHttpClient.newInstance("Mozilla/5.0 (X11; Ubuntu; Linux i686; rv:29.0) Gecko/20100101 Firefox/29.0");
                    HttpResponse response = httpClient.execute(httpGet,localContext);
                    HttpEntity entity = response.getEntity();
                    String sreturned = "";

                    if (entity != null) {
                        InputStream instream = entity.getContent();
                        // check if the response is gzipped
                        Header encoding = response.getFirstHeader("Content-Encoding");
                        if (encoding != null && encoding.getValue().equals("gzip")) {
                            instream = new GZIPInputStream(instream);
                        }
                        sreturned = convertStreamToString(instream);
                        // Closing the input stream will trigger connection release
                        instream.close();
                    }

                    //Obtain CSRF from body
                    String csrf = "";
                    Pattern pcsrf = Pattern.compile("\"csrf\"*+[^\"]*+\"([^\"]+)\"|\"csfr\"*+[^\"]*+\"([^\"]+)\"");
                    Matcher mcsrf = pcsrf.matcher(sreturned);

                    if (mcsrf.find()) {
                        if (mcsrf.group(1) != null) {
                            csrf = mcsrf.group(1);
                        } else {
                            csrf = mcsrf.group(2);
                        }
                    }

                    if (LOGGING) Log.d(TAG, "CSRF = " + csrf);

                    HttpPost httpPost = new HttpPost("https://secure.tuenti.com/?m=Login&func=do_login");
                    httpPost.setHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
                    httpPost.setHeader("Referer", "https://www.tuenti.com/?m=Login");

                    List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(5);

                    nameValuePairs.add(new BasicNameValuePair("timezone", "1"));
                    nameValuePairs.add(new BasicNameValuePair("timestamp", "1"));
                    nameValuePairs.add(new BasicNameValuePair("email", user));
                    nameValuePairs.add(new BasicNameValuePair("input_password", password));
                    nameValuePairs.add(new BasicNameValuePair("csfr", csrf));
                    httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

                    httpPost.getParams().setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.NETSCAPE);

                    response = httpClient.execute(httpPost,localContext);
                    entity = response.getEntity();

                    if (entity != null) {
                        InputStream instream = entity.getContent();
                        // check if the response is gzipped
                        Header encoding = response.getFirstHeader("Content-Encoding");
                        if (encoding != null && encoding.getValue().equals("gzip")) {
                            instream = new GZIPInputStream(instream);
                        }
                        sreturned = convertStreamToString(instream);
                        // Closing the input stream will trigger connection release
                        instream.close();
                    }

                    httpGet = new HttpGet("https://www-1.tuenti.com/?m=Accountdashboard&func=index&utm_content=active_subscriber&utm_source=internal&utm_medium=mobile_tab&utm_campaign=cupcake_fixed&ajax=1");

                    httpGet.setHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
                    httpGet.setHeader("Accept-Language", "es-ES,es;q=0.8,en-US;q=0.5,en;q=0.3");
                    httpGet.setHeader("Accept-Encoding", "gzip, deflate");
                    httpGet.setHeader("Referer", "https://www.tuenti.com/?m=Login");
                    httpGet.setHeader("Connection", "Keep-alive");
                    httpGet.setHeader("Content-Type", "application/x-www-form-urlencoded");


                    response = httpClient.execute(httpGet,localContext);
                    entity = response.getEntity();

                    if (entity != null) {
                        InputStream instream = entity.getContent();
                        // check if the response is gzipped
                        Header encoding = response.getFirstHeader("Content-Encoding");
                        if (encoding != null && encoding.getValue().equals("gzip")) {
                            instream = new GZIPInputStream(instream);
                        }
                        sreturned = convertStreamToString(instream);
                        // Closing the input stream will trigger connection release
                        instream.close();
                    }


                    int counter = 0;
                    int sleepingTime = SLEEPING_TIME;
                    while (counter != COUNT_LIMIT) {
                        if (LOGGING) Log.d(TAG, "Sleeping " + sleepingTime + " s");
                        Thread.sleep(sleepingTime); //Wait data for being accessible and then request again
                        if (LOGGING) Log.d(TAG, "Time to wake up and ask again!");

                        httpGet = new HttpGet("https://www-1.tuenti.com/?m=Accountdashboard&func=index&utm_content=active_subscriber&utm_source=internal&utm_medium=mobile_tab&utm_campaign=cupcake_fixed&ajax=1");
                        httpGet.setHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
                        httpGet.setHeader("Accept-Language", "es-ES,es;q=0.8,en-US;q=0.5,en;q=0.3");
                        httpGet.setHeader("Accept-Encoding", "gzip, deflate");
                        httpGet.setHeader("Referer", "https://www.tuenti.com/?m=Login");
                        httpGet.setHeader("Connection", "Keep-alive");
                        httpGet.setHeader("Content-Type", "application/x-www-form-urlencoded");

                        response = httpClient.execute(httpGet,localContext);
                        entity = response.getEntity();

                        if (entity != null) {
                            InputStream instream = entity.getContent();
                            // check if the response is gzipped
                            Header encoding = response.getFirstHeader("Content-Encoding");
                            if (encoding != null && encoding.getValue().equals("gzip")) {
                                instream = new GZIPInputStream(instream);
                            }
                            sreturned = convertStreamToString(instream);
                            instream.close();
                        }

                        if ((sreturned.contains("tu-dash-balance")) &&
                                !(sreturned.contains(context.getResources().getString(R.string.loading))))
                            break;
                        counter++;
                        sleepingTime += sleepingTime;

                    }

                    if (counter == COUNT_LIMIT) {
                        if (LOGGING) Log.d(TAG, "data not found. :'(");
                        httpClient.close();
                        return result;
                    }

                    result = extractResult(sreturned);

                    httpClient.close();

                }catch(IOException e){
                    if (LOGGING) Log.e(TAG, "Error IOException");
                    e.printStackTrace();
                }catch(InterruptedException e){
                    if (LOGGING) Log.e(TAG, "Error IOException");
                    e.printStackTrace();
                }

                //Before finish store the results in the Map
                dataMap.put("dataMoney", result[0]);
                dataMap.put("dataNet", result[1]);
                dataMap.put("dataPercentage", result[2]);
                dataMap.put("dataVoiceNet", result[3]);
                dataMap.put("dataVoicePercentage", result[4]);
                dataMap.put("dataDays", result[5]);
                dataMap.put("dataDaysPercentage", result[6]);
                UnofficialTuentiWidgetConfigureActivity.saveData(context, dataMap, appWidgetId);
                return result;
            }
        }else{
            result[0] = "16.8 €";
            result[1] = "1 GB";
            result[2] = "100";
            result[3] = "30m";
            result[4] = "100";
            result[5] = "20d";
            result[6] = "100";
            return result;
        }
    }


    public void StoreResponse(String response, int counter){
        String finalString = "";
        StringBuffer fileData = new StringBuffer();
        File file = new File(context.getFilesDir(), "responses.txt");

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

            file = new File(context.getFilesDir(), "responses.txt");

            PrintWriter out = new PrintWriter(file);
            out.println(finalString);
            out.flush();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String[] extractResult(String sreturned){
        String result[] = {"","","","","","",""};
        if (LOGGING) Log.d(TAG, "Some data found! :)");

        String consumption = "";
        Pattern pc1 = Pattern.compile("\\\\u20ac\\\\u00a0([0-9]+?\\.[0-9]*)");//Cases:  €0.15
        Matcher mc1 = pc1.matcher(sreturned);
        Pattern pc2 = Pattern.compile("\\\\u20ac\\\\u00a0([0-9]+?)");//Cases:  €0 or  €4
        Matcher mc2 = pc2.matcher(sreturned);
        Pattern pc3 = Pattern.compile("([0-9]+?,[0-9]*)[^,]*\\\\u20ac");//Cases:  0,15€
        Matcher mc3 = pc3.matcher(sreturned);
        Pattern pc4 = Pattern.compile("([0-9]++)[^,0-9]*\\\\u20ac");//Cases:  0€ or 4€
        Matcher mc4 = pc4.matcher(sreturned);

        if (mc1.find()) {
            if (LOGGING) Log.d(TAG, "group1 = " + mc1.group(1));
            consumption = mc1.group(1);
        } else if (mc2.find()) {
            if (LOGGING) Log.d(TAG, "group2 = " + mc2.group(1));
            consumption = mc2.group(1);
        } else if (mc3.find()) {
            if (LOGGING) Log.d(TAG, "group3 = " + mc3.group(1));
            consumption = mc3.group(1);
        } else if (mc4.find()) {
            if (LOGGING) Log.d(TAG, "group4 = " + mc4.group(1));
            consumption = mc4.group(1);
        } else {
            //No money
            result[0] = "badAccount";
        }

        consumption = consumption.replace(',', '.');
        double consumptionDouble = (double) Math.round(((Double.parseDouble(consumption) *
                (1 + Double.parseDouble(dataVAT))) +
                Double.parseDouble(dataBundlePrice)) * 100) / 100;
        if (LOGGING) Log.d(TAG, consumptionDouble + " €");
        result[0] = consumptionDouble + " €";

        String percentage = "";
        Pattern patternPER = Pattern.compile("percentage\\\":(\\d+).?");
        Matcher matcherPER = patternPER.matcher(sreturned);


        if (matcherPER.find()) {
            percentage = matcherPER.group(1);
            result[6] = percentage;//percentage of the month consumed
            if (LOGGING) Log.d(TAG, percentage + " %");
            //percentage found ... It must be the percentage (in days of a month) of the plan left
            Pattern daysPattern = Pattern.compile("([0-9]{1,2}+)\\D*d\\\\u00edas");
            Matcher daysMatcher = daysPattern.matcher(sreturned);

            if (daysMatcher.find()) {
                if (LOGGING) Log.d(TAG, "days = " + daysMatcher.group(1));
                result[5] = daysMatcher.group(1) + " d";;
            }else{
                result[5]="";
            }
        }

        if (matcherPER.find()) {
            percentage = matcherPER.group(1);
            result[2] = percentage;
            if (LOGGING) Log.d(TAG, percentage + " %");

            //second percentage found ... It should exists bundle data
            //Pattern patternMB = Pattern.compile("([0-9]{1,3}+)[^0-9]*>MB ");
            //Pattern patternGB = Pattern.compile("([0-9]{1,2}+)\\.([0-9]{3}+)[^0-9]*>MB ");
            Pattern patternMB = Pattern.compile("\\D([0-9]{1,3}+)\\D*MB");
            Pattern patternGB = Pattern.compile("([0-9]{4}+)\\D*MB");
            Matcher matcherMB = patternMB.matcher(sreturned);
            Matcher matcherGB = patternGB.matcher(sreturned);
            if (matcherGB.find()) {
                double gigasDouble = (double) Math.round((Double.parseDouble(matcherGB.group(1)) / 1024) * 100) / 100;
                if (LOGGING) Log.d(TAG, +gigasDouble + " GB");
                result[1] = gigasDouble + " GB";
            } else if (matcherMB.find()) {
                result[1] = matcherMB.group(1) + " MB";
                if (LOGGING) Log.d(TAG,"data = " + result[1] + " MB");
            } else {
                //No bundle
                result[1] = "";
            }
        }

        if (matcherPER.find()) {
            percentage = matcherPER.group(1);
            result[4] = percentage;
            if (LOGGING) Log.d("UTuentiW,NetworkTask voice = ", percentage + " %");

            //percentage found ... It should exists bundle data

            Pattern patternMin = Pattern.compile("([0-9]{1,3}+)[^0-9]*>min ");
            Matcher matcherMin = patternMin.matcher(sreturned);
            if (matcherMin.find()) {
                result[3] = matcherMin.group(1) + " m";
                if (LOGGING) Log.d(TAG,"data = " + matcherMin.group(1) + "min");

            } else {
                //No bundle
                result[3] = "";
            }
        }else{
            result[3] = null;
            result[4] = null;
        }





        return result;
    }


    private static String convertStreamToString(InputStream is) {

        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();

        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }

    @Override
    protected void onPostExecute(String[] result) {
        if (LOGGING) Log.d(TAG,"Begin");
        remoteViews.setViewVisibility(R.id.ProgressBarLayout, View.GONE);
        if (LOGGING) Log.d(TAG,"ProgressBar GONE, appWidgetId = " + appWidgetId);
        updateRemoteViews(result);
    }

    public void updateRemoteViews (String[] result){
        if (LOGGING) Log.d(TAG,"Begin");
        remoteViews.setViewVisibility(R.id.ProgressBarLayout, View.GONE);
        if (LOGGING) Log.d(TAG,"ProgressBar GONE, appWidgetId = " + appWidgetId);

        if(result != null || !result[0].equals("") || !result[1].equals("") || !result[2].equals("")){
            if (LOGGING) Log.d(TAG,"result[0]" + result[0]);
            if (LOGGING) Log.d(TAG,"result[1]" + result[1]);
            if (LOGGING) Log.d(TAG,"result[2]" + result[2]);
            if (LOGGING) Log.d(TAG,"result[3]" + result[3]);
            if (LOGGING) Log.d(TAG,"result[4]" + result[4]);
            if (LOGGING) Log.d(TAG,"result[5]" + result[5]);
            if (LOGGING) Log.d(TAG,"result[6]" + result[6]);

            if(result[2]==null){
                result[2]="100";
            }

            if(result[4]==null){
                result[4]="100";
            }

            //squareSide = 200;
            if (LOGGING) Log.d(TAG, "" + squareSide);

            Bitmap bitmap = Bitmap.createBitmap(squareSide, squareSide, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            int borderSize = 0;
            int innerSize = 0;
            boolean withVoice;

            if(result[3]=="0 m") {
                //Only data info **********************
                withVoice = false;

                //borderSize = (int) (squareSide * 0.03);
                //innerSize = (int) (squareSide * 0.07);
                borderSize = (int) (squareSide * 0.02);
                innerSize = (int) (squareSide * 0.12);

                Paint p1 = new Paint();
                p1.setAntiAlias(true);
                p1.setFilterBitmap(true);
                p1.setDither(true);
                p1.setColor(Color.parseColor("#00A5FF"));
                RectF rectF1 = new RectF(0, 0, squareSide, squareSide);
                canvas.drawArc (rectF1, 0, 360, true, p1);

                Paint p2 = new Paint();
                p2.setAntiAlias(true);
                p2.setFilterBitmap(true);
                p2.setDither(true);
                p2.setColor(Color.WHITE);
                RectF rectF2 = new RectF(borderSize, borderSize, squareSide-borderSize, squareSide-borderSize);
                canvas.drawArc (rectF2, 0, 360, true, p2);

                Paint p3 = new Paint();
                p3.setAntiAlias(true);
                p3.setFilterBitmap(true);
                p3.setDither(true);

                int[] colors_p3 = {Color.parseColor("#00A5FF"),
                                   Color.parseColor("#5AEB00"),
                                   Color.parseColor("#00A5FF"),
                                   Color.parseColor("#5AEB00")};
                float angle_p3 = (float) (Float.parseFloat(result[2]) * 3.6);
                float[] positions_p3 = {0, (angle_p3/360f) / 2f, angle_p3/360f, 1};
                SweepGradient gradient_p3 = new SweepGradient(squareSide/2, squareSide/2, colors_p3 , positions_p3);
                Matrix gm_3 = new Matrix();
                gradient_p3.getLocalMatrix(gm_3);
                gm_3.postRotate(-90,squareSide/2, squareSide/2);
                gradient_p3.setLocalMatrix(gm_3);
                p3.setShader(gradient_p3);
                RectF rectF3 = new RectF(borderSize, borderSize, squareSide - borderSize, squareSide - borderSize);
                canvas.drawArc(rectF3, 270, (int) (Double.parseDouble(result[2]) * 3.6), true, p3);

                Paint p4 = new Paint();
                p4.setAntiAlias(true);
                p4.setFilterBitmap(true);
                p4.setDither(true);
                p4.setColor(Color.parseColor("#00A5FF"));
                RectF rectF4 = new RectF(borderSize + innerSize,
                        borderSize + innerSize,
                        squareSide-(borderSize + innerSize),
                        squareSide-(borderSize + innerSize));
                canvas.drawArc (rectF4, 0, 360, true, p4);

                Paint p5 = new Paint();
                p5.setAntiAlias(true);
                p5.setFilterBitmap(true);
                p5.setDither(true);
                p5.setColor(Color.WHITE);
                RectF rectF5 = new RectF(borderSize * 2 + innerSize,
                        borderSize * 2 + innerSize,
                        squareSide-(borderSize * 2 + innerSize),
                        squareSide-(borderSize * 2 + innerSize));
                canvas.drawArc (rectF5,  0, 360, true, p5);

            }else{
                //Data and Voice **********************
                withVoice = true;
                borderSize = (int) (squareSide * 0.01);
                innerSize = (int) (squareSide * 0.06);

                Paint p1 = new Paint();
                p1.setAntiAlias(true);
                p1.setFilterBitmap(true);
                p1.setDither(true);
                p1.setColor(Color.parseColor("#00A5FF"));//BORDER (BLUE)
                RectF rectF1 = new RectF(0, 0, squareSide, squareSide);
                canvas.drawArc (rectF1, 0, 360, true, p1);

                //Data
                Paint p2 = new Paint();
                p2.setAntiAlias(true);
                p2.setFilterBitmap(true);
                p2.setDither(true);
                p2.setColor(Color.WHITE);
                RectF rectF2 = new RectF(borderSize,
                                         borderSize,
                                         squareSide-borderSize,
                                         squareSide-borderSize);
                canvas.drawArc (rectF2, 0, 360, true, p2);


                Paint p31 = new Paint();
                p31.setAntiAlias(true);
                p31.setFilterBitmap(true);
                p31.setDither(true);
                //INNER (BLUE-TO-GREEN)
                int[] colors_p31 = {Color.parseColor("#00A5FF"),
                                    Color.parseColor("#5AEB00"),
                                    Color.parseColor("#00A5FF"),
                                    Color.parseColor("#5AEB00")};
                float angle_p31 = (float) (Float.parseFloat(result[2]) * 3.6);
                float[] positions_p31 = {0, (angle_p31/360f) / 2f, angle_p31/360f, 1};
                SweepGradient gradient_p31 = new SweepGradient(squareSide/2, squareSide/2, colors_p31 , positions_p31);
                Matrix gm_31 = new Matrix();
                gradient_p31.getLocalMatrix(gm_31);
                gm_31.postRotate(-90,squareSide/2, squareSide/2);
                gradient_p31.setLocalMatrix(gm_31);
                p31.setShader(gradient_p31);
                RectF rectF31 = new RectF(borderSize / 2,
                                          borderSize / 2,
                                          squareSide - (borderSize / 2),
                                          squareSide - (borderSize / 2));
                canvas.drawArc(rectF31, 270, (int) (Double.parseDouble(result[2]) * 3.6), true, p31);

                //Voice
                Paint p32 = new Paint();
                p32.setAntiAlias(true);
                p32.setFilterBitmap(true);
                p32.setDither(true);
                p32.setColor(Color.WHITE);
                RectF rectF32 = new RectF(2 * borderSize + innerSize,
                                          2 * borderSize + innerSize,
                                          squareSide-(2 * borderSize + innerSize),
                                          squareSide-(2 * borderSize + innerSize));
                canvas.drawArc (rectF32, 0, 360, true, p32);


                Paint p33 = new Paint();
                p33.setAntiAlias(true);
                p33.setFilterBitmap(true);
                p33.setDither(true);
                //INNER (ORANGE-TO-RED)
                int[] colors_p33 =  {Color.parseColor("#FFC400"),
                                     Color.parseColor("#FF0095"),
                                     Color.parseColor("#FFC400"),
                                     Color.parseColor("#FF0095")};

                float angle_p33 = (float) (Float.parseFloat(result[4]) * 3.6);
                float[] positions_p33 = {0, (angle_p33/360f) / 2f, angle_p33/360f, 1};
                SweepGradient gradient_p33 = new SweepGradient(squareSide/2, squareSide/2, colors_p33 , positions_p33);
                Matrix gm_33 = new Matrix();
                gradient_p33.getLocalMatrix(gm_33);
                gm_33.postRotate(-90, squareSide/2, squareSide/2);
                gradient_p33.setLocalMatrix(gm_33);
                p33.setShader(gradient_p33);
                RectF rectF33 = new RectF(2 * borderSize + innerSize,
                                          2 * borderSize + innerSize,
                                          squareSide-(2 * borderSize + innerSize),
                                          squareSide-(2 * borderSize + innerSize));
                canvas.drawArc(rectF33, 270, (int) (Double.parseDouble(result[4]) * 3.6), true, p33);

                Paint p4 = new Paint();
                p4.setAntiAlias(true);
                p4.setFilterBitmap(true);
                p4.setDither(true);
                p4.setColor(Color.parseColor("#00A5FF"));//BLUE
                RectF rectF4 = new RectF(3 * borderSize + 2 * innerSize,
                                         3 * borderSize + 2 * innerSize,
                                         squareSide-(3 * borderSize + 2 * innerSize),
                                         squareSide-(3 * borderSize + 2 * innerSize));
                canvas.drawArc (rectF4, 0, 360, true, p4);

                Paint p5 = new Paint();
                p5.setAntiAlias(true);
                p5.setFilterBitmap(true);
                p5.setDither(true);
                p5.setColor(Color.WHITE);

                RectF rectF5 = new RectF(4 * borderSize + 2 * innerSize,
                                         4 * borderSize + 2 * innerSize,
                                         squareSide-(4 * borderSize + 2 * innerSize),
                                         squareSide-(4 * borderSize + 2 * innerSize));
                canvas.drawArc (rectF5,  0, 360, true, p5);
            }


            remoteViews.setImageViewBitmap(R.id.annulus, bitmap);


            //TEXT SECTION
            int moneySize = 0;
            int voiceSize = 0;
            int daysSize = 0;
            int refDim;
            if (withVoice){
                refDim = innerSize;
            }else{
                refDim = innerSize / 2;
            }

            if(result[5]!=null && result[5] != "") {
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                    if(result[5].length() != 0) {
                        if (landscape){
                            daysSize = (int) ((squareSide - (refDim * 10)) / result[5].length());
                        }else {
                            daysSize = (int) ((squareSide - (refDim * 9)) / result[5].length());
                        }
                    }
                }
                remoteViews.setTextViewText(R.id.dataDays, result[5]);
            }else{
                withVoice = false;
            }


            if(result[3]!=null && result[3] != "") {
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                    if(result[3].length() != 0) {
                        if (landscape){
                            voiceSize = (int) ((squareSide - (refDim * 10)) / result[3].length());
                        }else {
                            voiceSize = (int) ((squareSide - (refDim * 9)) / result[3].length());
                        }
                    }
                }
                remoteViews.setTextViewText(R.id.dataVoice, result[3]);
            }else{
                withVoice = false;
            }

            if(result[0]!=null && result[0] != "") {
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                    if (withVoice == true) {
                        if (landscape){
                            moneySize = (int) ((squareSide - ( refDim * 8)) / result[0].length());
                        }else {
                            moneySize = (int) ((squareSide - ( refDim * 5)) / result[0].length());
                        }
                    }else{
                        if (landscape){
                            moneySize = (int) ((squareSide - (refDim * 7)) / result[0].length());
                        }else {
                            moneySize = (int) ((squareSide - (refDim * 5)) / result[0].length());
                        }
                    }
                }
            }

            if(withVoice) {
                if (voiceSize < moneySize) {
                    moneySize = voiceSize;
                } else {
                    voiceSize = moneySize;
                    daysSize = moneySize;
                }
            }

            if(withVoice){
                remoteViews.setViewVisibility(R.id.dataVoice, View.VISIBLE);

                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                    if(result[3].length() != 0) {
                        if (LOGGING) Log.d(TAG, "min size = " + voiceSize);
                        remoteViews.setTextViewTextSize(R.id.dataVoice, TypedValue.COMPLEX_UNIT_PX, voiceSize);
                    }
                    if(result[5].length() != 0) {
                        if (LOGGING) Log.d(TAG, "days size = " + daysSize);
                        remoteViews.setTextViewTextSize(R.id.dataDays, TypedValue.COMPLEX_UNIT_PX, daysSize);
                    }
                }
                remoteViews.setTextViewText(R.id.dataVoice, result[3]);
            }else{
                remoteViews.setViewVisibility(R.id.dataVoice, View.GONE);
            }


            if(result[0]!=null && result[0] != "") {
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                    remoteViews.setTextViewTextSize(R.id.dataMoney, TypedValue.COMPLEX_UNIT_PX, moneySize);
                    if (LOGGING) Log.d(TAG, "money size = " + moneySize);
                }
                remoteViews.setTextViewText(R.id.dataMoney, result[0]);
            }else{
                remoteViews.setTextViewText(R.id.dataMoney, "0 €");
            }



            if(result[1]!=null && result[1] != "") {
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                    if(result[1].length() != 0) {
                        int dataSize;
                        if (withVoice == true) {
                            if (landscape){
                                dataSize = (int) ((squareSide - (refDim * 7)) / result[1].length());
                            }else {
                                dataSize = (int) ((squareSide - (refDim * 5)) / result[1].length());
                            }
                        }else{
                            if (landscape){
                                dataSize = (int) ((squareSide - (refDim * 6)) / result[1].length());
                            }else {
                                dataSize = (int) ((squareSide - (refDim * 5)) / result[1].length());
                            }
                        }

                        if (LOGGING) Log.d(TAG, "data size = " + dataSize);
                        remoteViews.setTextViewTextSize(R.id.dataNet, TypedValue.COMPLEX_UNIT_PX, dataSize);
                    }
                }
                remoteViews.setTextViewText(R.id.dataNet, result[1]);
            }else{
                remoteViews.setTextViewText(R.id.dataNet, context.getString(R.string.nobundle));
            }



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
            if(result[3]!=null) {
                remoteViews.setOnClickPendingIntent(R.id.dataVoice, pendingIntentForceUpdate);
            }

            // Instruct the widget manager to update the widget
            appWidgetManager.updateAppWidget(appWidgetId, remoteViews);

        }else{
            if (LOGGING) Log.d(TAG,"Text empty");
        }

    }


}


