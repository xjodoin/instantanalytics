package com.instantanalytics;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

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
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ListView;
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
		
		TextView dayInfo =  (TextView) findViewById(R.id.date);
		Date date = new Date();
		dayInfo.setText(DateFormat.getDateInstance(DateFormat.FULL).format(date));
		try {
			showDayData(date,profileId);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	private void showDayData(Date date ,String profileId) throws IOException {
		
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
		String dateString = simpleDateFormat.format(date);
		
		Get apiQuery = client.data().ga().get(
			    "ga:"+profileId,                       // "ga:" + Profile Id.
			    dateString,                   // Start date.
			    dateString,                   // End date.
			    "ga:visits,ga:pageviews,ga:timeOnSite,ga:exits");      // Metrics.
		
		apiQuery.setDimensions("ga:source,ga:medium");
//		apiQuery.setFilters("ga:medium==referral");
		apiQuery.setSort("-ga:visits");
//		apiQuery.setSegment("gaid::-11");
		apiQuery.setMaxResults(50);

		GaData execute = apiQuery.execute();
		
		Map<String, String> totals = execute.getTotalsForAllResults();
		List<String> data = new ArrayList<String>();
		data.add("Visits: "+totals.get("ga:visits"));
		data.add("Page views: "+totals.get("ga:pageviews"));

		List<List<String>> rows = execute.getRows();
		
		for (List<String> row : rows) {
			data.add(row.get(0)+": "+row.get(2));
		}
		
		getListView().setAdapter(new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, data));
	}
	
	private ListView getListView() {
		return (ListView) findViewById(R.id.data);
	}
	
}
