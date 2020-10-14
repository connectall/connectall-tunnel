package com.g2g.rest;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.RequestHeader;

import com.g2g.model.User;

import com.github.wnameless.json.flattener.JsonFlattener;
import com.github.wnameless.json.unflattener.JsonUnflattener;

@Path("/api/")
public class SessionService {

	private static Properties p = null;

	private void loadProperties() throws FileNotFoundException, IOException {
		p = new Properties();
		String webAppRoot = System.getProperty( "catalina.base" );
		String s = File.separator;
		String configDir = webAppRoot + s + "conf";
		String configFile = configDir + s + "connectall-tunnel.properties";
		p.load(new FileInputStream(configFile));
		url = (String) p.get("url");
		postCommentUrl = (String) p.get("comment_url");
		userCredentials = (String) p.getProperty("auth");
		//apiKey = (String) p.getProperty("apiKey");
	}
	static String url;
	static String userCredentials;
	static String postCommentUrl;
	//static String apiKey;


	private final org.apache.commons.logging.Log logger = LogFactory.getLog(SessionService.class);

	@POST @Consumes("Application/json") @Produces("application/json")
	@Path("/push/{appLink}")
	public Response push(@QueryParam("apiKey") String apiKey, @PathParam("appLink") String appLink, final String input) {

		int status = 201;
		try {

			loadProperties();

			logger.info("BitBucket request. Inputs: "+input);
			JSONObject result = new JSONObject();
			//Map<String,String> fields = new HashMap<String,String>();
			JSONObject args = new JSONObject(input);


			//String flattenedJson = JsonFlattener.flatten(input);
			//logger.info("\n=====Simple Flatten===== \n" + flattenedJson);

			Map<String, Object> fields = JsonFlattener.flattenAsMap(input);
			//fields = reformatDates(fields);
			String issueKey = addIssueKey(fields);
			fields.put("issuekey", issueKey);
			result.put("appLinkName", appLink);
			result.put("fields", fields);
			logger.info("The result is: " + result);

			PostRecordApi.postRecord(url, userCredentials, apiKey, result.toString());
			return Response.status(status)
					.build();
		} catch (Exception e) {
			e.printStackTrace();
			return Response.status(500).entity("{\"error\":\""+e.getMessage()+"\"}").build();
		}

	}

	@POST @Consumes("Application/json") @Produces("application/json")
	@Path("/postComment/{appLink}")
	public Response postComment(@QueryParam("apiKey") String apiKey, @PathParam("appLink") String appLink, final String input) {

		int status = 201;
		String recordId = "";
		String issueKey = null;
		JSONArray commentList = new JSONArray();
		try {

			loadProperties();

			logger.info("BitBucket request. Inputs: "+input);
			JSONObject result = new JSONObject();
			//Map<String,String> fields = new HashMap<String,String>();
			JSONObject args = new JSONObject(input);
			JSONObject push = args.getJSONObject("push");
			JSONArray changes = push.getJSONArray("changes");
			for (int j=0; j < changes.length(); j++) {
				JSONObject change = changes.getJSONObject(j);
				logger.info("Change = " + change);
				JSONArray commits = change.getJSONArray("commits");
				for (int i=0; i<commits.length(); i++) {
					Map<String,String> fields = new HashMap<String,String>();
					JSONObject commit = commits.getJSONObject(i); 
					recordId = commit.getString("hash");
					fields.put("author", commit.getJSONObject("author").getString("raw"));
					String title = commit.getJSONObject("summary").getString("raw");
					if (issueKey == null)
						issueKey = addIssueKey(title);
					String date = commit.getString("date");
					String diff = commit.getJSONObject("links").getJSONObject("diff").getString("href");
					fields.put("body",  recordId+"\n"+date+"\n"+title+"\n"+diff);
					commentList.put(fields);
				}	// end for each commit			

				result.put("recordId", recordId);
				result.put("appLinkName", appLink);
				result.put("commentList", commentList);
				logger.info("The result is: " + result);


				// link the two records in the connectall database
				JSONObject linker = new JSONObject();
				Map linkerFields = new HashMap();
				linkerFields.put("issuekey",  issueKey);
				linkerFields.put("id", issueKey);
				linker.put("fields",  new JSONObject(linkerFields));
				linker.put("appLinkName", appLink);
				logger.info("Linker record body="+linker);
				PostRecordApi.postRecord(url, userCredentials, apiKey, linker.toString());

				// Update the remote endpoint with the checkin comments
				PostRecordApi.postRecord(postCommentUrl, userCredentials, apiKey, result.toString());
				return Response.status(status)
						.build();

			}
			return Response.status(500).entity("{\"error\":\"Message received is not a valid push\"}").build();



		} catch (Exception e) {
			e.printStackTrace();
			return Response.status(500).entity("{\"error\":\""+e.getMessage()+"\"}").build();
		}

	}

	DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX");
	DateFormat df2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	private String addIssueKey(Map<String,Object> fields) {
		for (Iterator<String> iter = fields.keySet().iterator(); iter.hasNext(); ) {
			String k = iter.next();
			if (k.endsWith("title")) {
				String v = (String) fields.get(k);
				if (v != null && v.length() > 0)
					return v.split(" ")[0];
			}
		}
		return "";
	}
	private String addIssueKey(String v) {
					return v.split(" ")[0];
	}
	private Map<String,Object> reformatDates(Map<String,Object> fields) {
		//		Map<String,Object> result = new HashMap<String,Object>();
		try {
			for (Iterator<String> iter = fields.keySet().iterator(); ;iter.hasNext()) {
				String k = iter.next();
				String v = (String) fields.get(k);
				try {
					Date d = df.parse(v);
					fields.put(k, df2.format(d));
				} catch  (Exception e) {
					fields.put(k, v);
					//logger.info("Could not reformat data field due to the error " + e);
				} 
			}

		} catch (Exception e) {
			logger.info("Could not reformat data field due to the error " + e);
		}
		return fields;
	}
	private void saveScalar (Map<String,String> result, String fieldName, JSONObject field) throws JSONException {
		result.put(fieldName, field.getString(fieldName));
	}

	private String getDate(String s) throws ParseException {
		logger.info("Parse the date " + s);
		Date d = df.parse(s);
		return df2.format(d);
	}
	private User getUser(JSONObject s) throws JSONException {
		User result = new User();
		result.setName(s.getString("nickname"));
		return result;
	}

}
