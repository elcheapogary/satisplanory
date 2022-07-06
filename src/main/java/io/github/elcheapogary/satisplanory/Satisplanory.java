/*
 * Copyright (c) 2022 elcheapogary
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package io.github.elcheapogary.satisplanory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Satisplanory
{
    private static Properties versionProperties = null;

    private Satisplanory()
    {
    }

    public static String getApplicationName()
    {
        return "Satisplanory";
    }

    public static String getLatestReleasedVersion()
            throws IOException, InterruptedException
    {
        URI uri;

        try {
            uri = new URI("https://api.github.com/repos/elcheapogary/satisplanory/releases");
        }catch (URISyntaxException e){
            throw new IOException("Supposedly something wrong with the URL syntax, bah humbug!", e);
        }

        HttpRequest request = HttpRequest.newBuilder(uri)
                .GET()
                .header("Accept", "application/vnd.github.v3+json")
                .build();

        HttpResponse<String> response;

        try {
            response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        }catch (IOException e){
            throw new IOException("Error making HTTP request", e);
        }

        if (response.statusCode() != 200){
            throw new IOException("Response code: " + response.statusCode());
        }

        try {
            JSONArray jsonArray = new JSONArray(response.body());

            JSONObject latestRelease = jsonArray.getJSONObject(0);

            String tag = latestRelease.getString("tag_name");

            if (tag.startsWith("v")){
                return tag.substring(1);
            }

            return tag;
        }catch (JSONException e){
            throw new IOException("Error parsing JSON data", e);
        }
    }

    public static String getVersion()
    {
        try {
            return loadVersionProperties().getProperty("version");
        }catch (IOException e){
            throw new RuntimeException("Error loading version properties", e);
        }
    }

    public static boolean isDevelopmentVersion()
    {
        return getVersion().endsWith("-SNAPSHOT");
    }

    private static Properties loadVersionProperties()
            throws IOException
    {
        synchronized (Satisplanory.class){
            if (versionProperties == null){
                Properties tmp = new Properties();
                InputStream in = Satisplanory.class.getResourceAsStream("version.properties");
                if (in == null){
                    throw new IOException("Missing resource: version.properties");
                }
                try (Reader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                    tmp.load(r);
                }
                versionProperties = tmp;
            }
            return versionProperties;
        }
    }
}
