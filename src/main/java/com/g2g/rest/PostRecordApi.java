package com.g2g.rest;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.LogFactory;

public class PostRecordApi {

private static final org.apache.commons.logging.Log logger = LogFactory.getLog(PostRecordApi.class);

public static void postRecord(String url, String userCredentials, String apiKey, String body) throws Exception {
  logger.info("Send a request to " + url + " using the credentials " + userCredentials);
  URL obj = new URL(url);
  HttpURLConnection con = (HttpURLConnection) obj.openConnection();
  String input = body;

  String basicAuth = "Basic " + new String(Base64.getEncoder().encode(userCredentials.getBytes()));

  con.setRequestProperty ("Authorization", basicAuth);
  con.setRequestMethod("POST");
  con.setRequestProperty("Content-Type", "application/json");
  con.setRequestProperty("Accept", "application/json");
  con.setRequestProperty ("apikey", apiKey);

  con.setDoInput(true);
  con.setDoOutput(true);
  OutputStream os = con.getOutputStream();
  os.write(input.getBytes());
  os.flush();

  int responseCode = con.getResponseCode();
  logger.info("Method Response Code :: " + responseCode);

  if (responseCode == HttpURLConnection.HTTP_OK || responseCode == 201) { //success
    BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
    String inputLine;
    StringBuffer response = new StringBuffer();

    while ((inputLine = in.readLine()) != null) {
      response.append(inputLine);
    }
    in.close();
    logger.info(response.toString());
  } else {
	  logger.info("Method request not worked");
  }
}
}