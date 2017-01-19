package com.quaap.launchtime;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

public class FeedbackActivity extends Activity {

    private Map<String,String> manglednames = new HashMap<>();
    private Map<String,Boolean> includes = new HashMap<>();
    List<AppShortcut> apps = new ArrayList<>();
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
        txtappname.setText(appname + " " + version);

        TextView txtappver = (TextView) findViewById(R.id.info_app_version);
        txtappver.setText(version);


        DB db = ((GlobState)getApplicationContext()).getDB();
        for (String activityname: db.getAppActvNames()) {
            AppShortcut app = db.getApp(activityname);
            apps.add(app);
            if (app.isActionLink() || app.isLink()) {
                String uri = app.getLinkUri();
                if (uri!=null && uri.length()>6) {
                    uri = uri.substring(0,6) + "." + uri.hashCode();
                }
                String mangled = app.getLinkBaseActivityName() + "/" + uri;
                manglednames.put(activityname, mangled);
                activityname = app.getLinkBaseActivityName() + "/" + uri;
            } else {
                manglednames.put(activityname, activityname);
            }
            includes.put(activityname, true);

        }

        ListView itemsList = (ListView)findViewById(R.id.info_data_items);

        itemsList.setAdapter(new PackageAdapter(this, apps));

    }

    private String sendData() {
        StringBuffer sb = getBuildSendData();
        //Log.d("SendData", sb.toString());

        String requestURL =  "http://10.0.0.5/appbackend/receivedata.php";
        HashMap<String, String> postDataParams = new HashMap<>();

        postDataParams.put("app",appname);
        postDataParams.put("data", sb.toString());

        String response = sendPostData(requestURL, postDataParams);

        Log.d("SendData", response);

        String message;
        if (response.trim().equals("1")) {
            message = "Data successfully sent. Thanks!";
        } else {
            message = "Data tranfer failed:" + response;
        }
        return message;
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

    @NonNull
    private StringBuffer getBuildSendData() {
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
        for (AppShortcut app:apps) {
            String activityname = manglednames.get(app.getActivityName());
            Boolean checked = includes.get(activityname);
            if (checked==null || checked){
                sb.append(activityname);
                sb.append(":");
                sb.append(app.getCategory());
                sb.append("\n");
            }
        }
        sb.append("END APP DATA\n");
        return sb;
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

    class PackageAdapter extends ArrayAdapter<AppShortcut> {

        public PackageAdapter(Context context, List<AppShortcut> objects) {
            super(context, 0, objects);
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            // Get the data item for this position
            final AppShortcut app = getItem(position);
            if (app==null) {
                return convertView;
            }
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.line_item, parent, false);
            }

            CheckBox includeit = (CheckBox)convertView.findViewById(R.id.info_include);
            TextView pcknameview = (TextView)convertView.findViewById(R.id.item_text);
            TextView catnameview = (TextView)convertView.findViewById(R.id.item_cat);
            catnameview.setText(app.getCategory());

            String activityname = manglednames.get(app.getActivityName());

            pcknameview.setText(activityname);


            includeit.setOnCheckedChangeListener(null);
            Boolean checked = includes.get(activityname);

            includeit.setChecked(checked);

            final String activityname2 = activityname;
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
