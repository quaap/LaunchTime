package com.quaap.launchtime_official.components;

import android.support.annotation.NonNull;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

/**
 * Copyright (C) 2017   Tom Kliethermes
 *
 * This file is part of LaunchTime and is is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation; either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 */
public class HttpUtils {

    @NonNull
    public static String sendPostData(String requestURL, HashMap<String, String> postDataParams) {
        URL url;
        String response = "";
        try {

            System.setProperty("http.keepAlive", "false");

            url = new URL(requestURL);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            try {
                conn.setRequestProperty("Connection", "close");
                conn.setReadTimeout(15000);
                conn.setConnectTimeout(15000);
                conn.setRequestMethod("POST");
                conn.setDoInput(true);
                conn.setDoOutput(true);

                OutputStream os = conn.getOutputStream();
                InputStream in = null;
                try {
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                    writer.write(getPostDataString(postDataParams));

                    writer.flush();
                    writer.close();
                    int responseCode = conn.getResponseCode();

                    in = conn.getInputStream();

                    if (responseCode == HttpsURLConnection.HTTP_OK) {
                        String line;
                        BufferedReader br = new BufferedReader(new InputStreamReader(in));
                        while ((line = br.readLine()) != null) {
                            response += line + "\n";
                        }
                        br.close();

                    } else {
                        response = "Code " + responseCode;
                    }

                } finally {
                    try {
                        InputStream er = conn.getErrorStream();
                        if (er != null) {
                            while(er.read()!=-1) {
                                int i=0;
                            }
                            er.close();
                        }
                    } catch (IOException e) {
                        Log.d("LaunchTime", "Http getErrorStream: " + e.getMessage());
                    }
                    if (in != null) try {
                        in.close();
                    } catch (IOException e) {
                        Log.d("LaunchTime", "in" + e.getMessage());
                    }
                    if (os != null) try {
                        os.close();
                    } catch (IOException e) {
                        Log.d("LaunchTime", "os" + e.getMessage());
                    }
                }

            } finally {
                conn.disconnect();
            }
        } catch (Exception e) {
            e.printStackTrace();
            response=e.getLocalizedMessage();
        }
        return response;
    }


    public static String getPostDataString(HashMap<String, String> params) throws UnsupportedEncodingException {
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

}
