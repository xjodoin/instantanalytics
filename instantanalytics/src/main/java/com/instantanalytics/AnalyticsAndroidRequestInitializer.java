package com.instantanalytics;

import java.io.IOException;

import android.content.SharedPreferences;

import com.google.api.client.googleapis.GoogleHeaders;
import com.google.api.client.googleapis.extensions.android2.auth.GoogleAccountManager;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;

public class AnalyticsAndroidRequestInitializer extends
		CalendarRequestInitializer {

	private String authToken;
	private final SharedPreferences settings;
	private final GoogleAccountManager accountManager;

	public AnalyticsAndroidRequestInitializer(HttpTransport transport,SharedPreferences settings,GoogleAccountManager accountManager) {
		super(transport);
		this.settings = settings;
		this.accountManager = accountManager;
		authToken = settings.getString(ProfilesActivity.PREF_AUTH_TOKEN, null);
		setGsessionid(settings.getString(ProfilesActivity.PREF_GSESSIONID, null));
	}

	@Override
	public void intercept(HttpRequest request) throws IOException {
		super.intercept(request);
		request.getHeaders().setAuthorization(
				GoogleHeaders.getGoogleLoginValue(authToken));
	}

	@Override
	public boolean handleResponse(HttpRequest request, HttpResponse response,
			boolean retrySupported) throws IOException {
		switch (response.getStatusCode()) {
		case 302:
			super.handleResponse(request, response, retrySupported);
			SharedPreferences.Editor editor = settings.edit();
			editor.putString(ProfilesActivity.PREF_GSESSIONID, getGsessionid());
			editor.commit();
			return true;
		case 401:
			accountManager.invalidateAuthToken(authToken);
			authToken = null;
			SharedPreferences.Editor editor2 = settings.edit();
			editor2.remove(ProfilesActivity.PREF_AUTH_TOKEN);
			editor2.commit();
			return false;
		}
		return false;
	}
	
	public void setAuthToken(String authToken) {
		this.authToken = authToken;
	}
	
	public String getAuthToken() {
		return authToken;
	}
}