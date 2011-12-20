package com.instantanalytics;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import com.google.api.client.extensions.android2.AndroidHttp;
import com.google.api.client.googleapis.extensions.android2.auth.GoogleAccountManager;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.json.JsonHttpRequest;
import com.google.api.client.http.json.JsonHttpRequestInitializer;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.services.analytics.Analytics;
import com.google.api.services.analytics.AnalyticsRequest;
import com.google.api.services.analytics.Analytics.Data.Ga.Get;
import com.google.api.services.analytics.model.GaData;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;

public class DayView extends Activity {

	private final HttpTransport transport = AndroidHttp.newCompatibleTransport();


	private GoogleAccountManager accountManager;
	private SharedPreferences settings;
	private AnalyticsAndroidRequestInitializer requestInitializer;
	
	private Analytics client;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		accountManager = new GoogleAccountManager(this);
		settings = this.getSharedPreferences(ProfilesActivity.PREF, 0);
		requestInitializer = new AnalyticsAndroidRequestInitializer(transport,
				settings, accountManager);
		client = Analytics
				.builder(transport, new JacksonFactory())
				.setApplicationName("InstantAnalytics/1.0")
				.setHttpRequestInitializer(requestInitializer)
				.setJsonHttpRequestInitializer(
						new JsonHttpRequestInitializer() {
							public void initialize(JsonHttpRequest request)
									throws IOException {
								AnalyticsRequest tasksRequest = (AnalyticsRequest) request;
								tasksRequest.setKey(ClientCredentials.KEY);
							}
						}).build();
		
		
		setContentView(R.layout.day);
		
		String profileId = getIntent().getExtras().getString("profileId");
		
		TextView dayInfo = (TextView) findViewById(R.id.dateInfo);
		Date date = new Date();
		java.text.DateFormat dateFormat = android.text.format.DateFormat.getDateFormat(getApplicationContext());
		dayInfo.setText(dateFormat.format(date));
		
		
		try {
			showDayData(profileId);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	private void showDayData(String profileId) throws IOException {
		Get apiQuery = client.data().ga().get(
			    "ga:"+profileId,                       // "ga:" + Profile Id.
			    "2011-12-08",                   // Start date.
			    "2011-12-08",                   // End date.
			    "ga:visits,ga:pageviews");      // Metrics.

		GaData execute = apiQuery.execute();
		
		TextView textView = (TextView) findViewById(R.id.analyticsData);
		
		List<String> entrySet = execute.getRows().get(0);

		textView.setText("Visits: "+entrySet.get(0)+"\n"+"Page views: "+entrySet.get(1));
	}
	
	
}
