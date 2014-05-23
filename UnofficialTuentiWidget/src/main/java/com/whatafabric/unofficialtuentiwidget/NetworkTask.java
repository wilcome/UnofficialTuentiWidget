package com.whatafabric.unofficialtuentiwidget;

/**
 * Created by Enrique García Orive on 22/05/14.
 */

import android.annotation.TargetApi;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;
import android.widget.RemoteViews;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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

    private static int numOfTries = 5;//Do not make rhyme
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

        //Log.d("NetworkTask ", "user = " + user);
        //Log.d("NetworkTask ", "password = " + password);
        Log.d("NetworkTask ", "dataMoney = " + dataMoney);
        Log.d("NetworkTask ", "dataNet = " + dataNet);
        Log.d("NetworkTask ", "dataPercentage = " + dataPercentage);

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
                    Log.d("NetworkTask CSRF = ", csrf);
                    return result;
                }
                int csrfEndIndex = sreturned.indexOf("\"", csrfStartIndex);
                csrf = sreturned.substring(csrfStartIndex, csrfEndIndex);
                Log.d("NetworkTask CSRF = ", csrf);


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
                Log.d("Network ", "status = " + status);

                link = "https://www-1.tuenti.com/?m=Accountdashboard&func=index&utm_content=active_subscriber&utm_source=internal&utm_medium=mobile_tab&utm_campaign=cupcake_fixed&ajax=1";

                int counter = 0;
                while (counter != numOfTries) {
                    counter++;
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
                    sreturned = sb2.toString();

                    if (sreturned.contains("tu-dash-balance")) {
                        Log.d("NetworkTask ", "data found! ;)");
                        String consumption = "";
                        Pattern paternConsumption = Pattern.compile("([0-9]+?,[0-9]*)[^,]*\\\\u20ac");
                        Matcher matcherConsumption = paternConsumption.matcher(sreturned);
                        if (matcherConsumption.find()) {
                            consumption = matcherConsumption.group(1);
                            Log.d("NetworkTask ", consumption + " €");
                            result[0] = consumption + " €";
                        }

                        String percentage = "";
                        Pattern paternPER = Pattern.compile("percentage\\\":(\\d+).?");
                        Matcher matcherPER = paternPER.matcher(sreturned);
                        if (matcherPER.find()) {
                            percentage = matcherPER.group(1);
                            result[2]=percentage;
                            Log.d("NetworkTask ", percentage + " %");

                            //percentage found ... It should exists bundle data
                            Pattern paternMB = Pattern.compile("([0-9]{1,4}+)[^0-9]*>MB ");
                            Pattern paternGB = Pattern.compile("([0-9]{1,4}+)[^0-9]*>GB ");
                            Matcher matcherMB = paternMB.matcher(sreturned);
                            Matcher matcherGB = paternGB.matcher(sreturned);
                            if (matcherMB.find()) {
                                String megaBytes = matcherMB.group(1);
                                if(megaBytes.length()>3) {
                                    double gigasDouble = (double)Math.round((Double.parseDouble(megaBytes) / 1024) * 100) / 100;
                                    Log.d("NetworkTask ",  + gigasDouble + " GB");
                                    result[1] = gigasDouble + " GB";
                                }else{
                                    Log.d("NetworkTask ", megaBytes + " MB");
                                    result[1] = megaBytes + " MB";
                                }
                            }else if (matcherGB.find()) {
                                result[1] = matcherGB.group(1) + " GB";
                            }else{
                                //No bundle
                                result[1]="";
                            }

                        }

                        break;
                    } else {
                        Thread.sleep(2000);
                        Log.d("NetworkTask ", "data still not found. :'(");
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
            Log.d("NetworkTask, onPostExecute ",result[0]+ " - " + result[1]);
            remoteViews.setTextViewText(R.id.dataMoney, result[0]);
            if(result[1]!="") {
                remoteViews.setTextViewText(R.id.dataNet, result[1]);
            }else{
                remoteViews.setTextViewText(R.id.dataNet, context.getString(R.string.nobundle));
            }

            if(result[2]!=""){
                Log.d("Network, onPostExecute ", "percentage = " + result[2]);

                int bgId = context.getResources().getIdentifier("tuenti_widget_"+result[2]+"_annulus",
                                                              "drawable",
                                                              context.getPackageName());
                remoteViews.setImageViewResource(R.id.annulus,bgId);
            }
            // Instruct the widget manager to update the widget
            appWidgetManager.updateAppWidget(appWidgetId, remoteViews);

        }else{
            Log.d("NetworkTask, onPostExecute ","Text empty");
        }
    }
}
