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
		userCredentials = (String) p.getProperty("auth");
		//apiKey = (String) p.getProperty("apiKey");
	}
	static String url;
	static String userCredentials;
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

	DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX");
	DateFormat df2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
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
