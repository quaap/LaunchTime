package com.quaap.launchtime;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.quaap.launchtime.components.AppShortcut;
import com.quaap.launchtime.db.DB;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipOutputStream;

import javax.net.ssl.HttpsURLConnection;

public class FeedbackActivity extends Activity {

    private LinkedHashMap<String,String> scrubbednames = new LinkedHashMap<>();
    private Map<String,Boolean> includes = new HashMap<>();
    List<AppShortcut> apps = new ArrayList<>();
    Map<String,AppShortcut> appMap = new HashMap<>();
    String version;
    String appname;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);

        appname = getString(R.string.app_name);
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            version = pInfo.versionName;
            loadData();
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        Button sendIt =(Button)findViewById(R.id.info_send);

        sendIt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AsyncTask<Void, Void, String> sendItAsync = new AsyncTask<Void, Void, String>() {

                    @Override
                    protected String doInBackground(Void... voids) {
                        return sendData();
                    }

                    @Override
                    protected void onPostExecute(String message) {
                        Toast.makeText(FeedbackActivity.this, message, Toast.LENGTH_SHORT).show();
                    }
                };
                sendItAsync.execute();
            }
        });

    }

    private void loadData() throws PackageManager.NameNotFoundException {


        TextView txtappname = (TextView) findViewById(R.id.info_app_name);
        txtappname.setText(appname);

        TextView txtappver = (TextView) findViewById(R.id.info_app_version);
        txtappver.setText(version);


        DB db = ((GlobState)getApplicationContext()).getDB();

        List<String> actnames = db.getAppActvNames();
        Collections.sort(actnames);

        for (String activityname: actnames) {
            AppShortcut app = db.getApp(activityname);
            if (app==null) continue;
            apps.add(app);
            appMap.put(activityname,app);

            int count = db.getAppLaunchedCount(activityname);

            String scrubbed;
            if (app.isActionLink() || app.isLink()) {
                String uri = app.getLinkUri();
                if (uri!=null && uri.length()>6) {
                    uri = uri.substring(0,6) + "." + uri.hashCode();
                }
                scrubbed = app.getLinkBaseActivityName() + "." + count + "/" + uri;
                //activityname = app.getLinkBaseActivityName() + "/" + uri;
            } else {
                scrubbed = activityname + "." + count;
            }
            
            scrubbednames.put(activityname, scrubbed);
            includes.put(scrubbed, true);

        }

        for (String catid: db.getCategories()) {
            String cat = "cat." + catid + "." + db.getCategoryDisplay(catid) + "." + db.isTinyCategory(catid);
            scrubbednames.put(cat, cat);
            includes.put(cat, true);
        }

        for (String actvname: db.getAppCategoryOrder(MainActivity.QUICK_ROW_CAT)) {
            String name = "qr." + scrubbednames.get(actvname);
            scrubbednames.put(name, name);
            includes.put(name, true);
        }
        ListView itemsList = (ListView)findViewById(R.id.info_data_items);

        itemsList.setAdapter(new PackageAdapter(this, new ArrayList<String>(scrubbednames.keySet())));

    }



    private String sendData() {
        StringBuffer sb = buildSendData();
        //Log.d("SendData", sb.toString());

        String requestURL =  getString(R.string.feedback_url);
        HashMap<String, String> postDataParams = new HashMap<>();

        postDataParams.put("app",appname);
        postDataParams.put("data", sb.toString());

        String response = sendPostData(requestURL, postDataParams);

        Log.d("SendData", response);

        String message;
        if (response.trim().equals("1")) {
            message = getString(R.string.sent_success);
        } else {
            message = getString(R.string.sent_failed) + response;
        }
        return message;
    }

    @NonNull
    private StringBuffer buildSendData() {
        StringBuffer sb = new StringBuffer(16000);
        sb.append(appname);
        sb.append(": ");
        sb.append(version);
        sb.append("\n");
        String comment = ((EditText)findViewById(R.id.info_user_message)).getText().toString();
        if (comment.length()>0) {
            sb.append("comment:");
            sb.append(Base64.encodeToString(comment.getBytes(),Base64.DEFAULT));
            //sb.append(comment);
            sb.append("\n");
        }
        sb.append("BEGIN APP DATA\n");
        for (String actvname: scrubbednames.keySet()) {
            String scrubbed = scrubbednames.get(actvname);
            Boolean checked = includes.get(scrubbed);
            if (checked==null || checked){
                sb.append(scrubbed);
                AppShortcut app = appMap.get(actvname);
                if (app!=null) {
                    sb.append(":");
                    sb.append(app.getCategory());
                }
                sb.append("\n");
            }
        }
        sb.append("END APP DATA\n");
        return sb;
    }



    @NonNull
    private String sendPostData(String requestURL, HashMap<String, String> postDataParams) {
        URL url;
        String response = "";
        try {
            url = new URL(requestURL);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(15000);
            conn.setConnectTimeout(15000);
            conn.setRequestMethod("POST");
            conn.setDoInput(true);
            conn.setDoOutput(true);

            OutputStream os = conn.getOutputStream();
            BufferedWriter writer = new BufferedWriter( new OutputStreamWriter(os, "UTF-8"));
            writer.write(getPostDataString(postDataParams));

            writer.flush();
            writer.close();
            os.close();
            int responseCode=conn.getResponseCode();

            if (responseCode == HttpsURLConnection.HTTP_OK) {
                String line;
                BufferedReader br=new BufferedReader(new InputStreamReader(conn.getInputStream()));
                while ((line=br.readLine()) != null) {
                    response+=line + "\n";
                }
            }
            else {
                response="Code " + responseCode;
            }
            conn.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
            response=e.getLocalizedMessage();
        }
        return response;
    }


    private String getPostDataString(HashMap<String, String> params) throws UnsupportedEncodingException {
        StringBuilder result = new StringBuilder();
        boolean first = true;
        for(Map.Entry<String, String> entry : params.entrySet()){
            if (first)
                first = false;
            else
                result.append("&");

            result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
            result.append("=");
            result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
        }

        return result.toString();
    }

    class PackageAdapter extends ArrayAdapter<String> {

        public PackageAdapter(Context context, List<String> objects) {
            super(context, 0, objects);
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            // Get the data item for this position
            String activityname = getItem(position);
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.line_item, parent, false);
            }

            CheckBox includeit = (CheckBox)convertView.findViewById(R.id.info_include);
            TextView pcknameview = (TextView)convertView.findViewById(R.id.item_text);
            TextView catnameview = (TextView)convertView.findViewById(R.id.item_cat);
            final AppShortcut app = appMap.get(activityname);
            if (app!=null) {
                catnameview.setText(app.getCategory());
            } else {
                catnameview.setText("na");
            }

            String scrubbed = scrubbednames.get(activityname);

            pcknameview.setText(scrubbed);


            includeit.setOnCheckedChangeListener(null);
            Boolean checked = includes.get(scrubbed);

            includeit.setChecked(checked);

            final String activityname2 = scrubbed;
            includeit.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    includes.put(activityname2, b);
                }
            });

            return convertView;
        }
    }
}
