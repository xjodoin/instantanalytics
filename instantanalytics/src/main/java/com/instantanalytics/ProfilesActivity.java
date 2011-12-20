/*
 * Copyright (c) 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.instantanalytics;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.google.api.client.extensions.android2.AndroidHttp;
import com.google.api.client.googleapis.GoogleHeaders;
import com.google.api.client.googleapis.extensions.android2.auth.GoogleAccountManager;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.json.JsonHttpRequest;
import com.google.api.client.http.json.JsonHttpRequestInitializer;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.services.analytics.Analytics;
import com.google.api.services.analytics.Analytics.Data.Ga.Get;
import com.google.api.services.analytics.AnalyticsRequest;
import com.google.api.services.analytics.model.GaData;
import com.google.api.services.analytics.model.Profile;
import com.google.common.base.Function;
import com.google.common.collect.Lists;

public final class ProfilesActivity extends Activity {

	/** Logging level for HTTP requests/responses. */
	private static Level LOGGING_LEVEL = Level.CONFIG;

	private static final String AUTH_TOKEN_TYPE = "analytics";

	private static final String TAG = "InstantAnalytics";

	private static final int MENU_ADD = 0;

	private static final int MENU_ACCOUNTS = 1;

	private static final int CONTEXT_EDIT = 0;

	private static final int CONTEXT_DELETE = 1;

	private static final int REQUEST_AUTHENTICATE = 0;

	private Analytics client;

	private final HttpTransport transport = AndroidHttp.newCompatibleTransport();

	String accountName;

	static final String PREF = TAG;
	static final String PREF_ACCOUNT_NAME = "accountName";
	static final String PREF_AUTH_TOKEN = "authToken";
	static final String PREF_GSESSIONID = "gsessionid";
	GoogleAccountManager accountManager;
	SharedPreferences settings;
	AnalyticsAndroidRequestInitializer requestInitializer;

	private List<Profile> profiles;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Logger.getLogger("com.google.api.client").setLevel(LOGGING_LEVEL);
		accountManager = new GoogleAccountManager(this);
		settings = this.getSharedPreferences(PREF, 0);
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

		setContentView(R.layout.main);

		ListView listView = getListView();
		listView.setTextFilterEnabled(true);

		listView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1,
					int position, long id) {
				Profile profile = profiles.get((int) id);
				try {
					showCurrentDay(profile);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});

		registerForContextMenu(getListView());
		gotAccount();

	}

	protected void showCurrentDay(Profile profile) throws IOException {

		Intent intent = new Intent(this, DayView.class);
		intent.putExtra("profileId", profile.getId());
		startActivity(intent);
	}

	private ListView getListView() {
		return (ListView) findViewById(R.id.profilesList);
	}

	void setAuthToken(String authToken) {
		SharedPreferences.Editor editor = settings.edit();
		editor.putString(PREF_AUTH_TOKEN, authToken);
		editor.commit();
		requestInitializer.setAuthToken(authToken);
	}

	void setAccountName(String accountName) {
		SharedPreferences.Editor editor = settings.edit();
		editor.putString(PREF_ACCOUNT_NAME, accountName);
		editor.remove(PREF_GSESSIONID);
		editor.commit();
		this.accountName = accountName;
		requestInitializer.setGsessionid(null);
	}

	private void gotAccount() {
		Account account = accountManager.getAccountByName(accountName);
		if (account != null) {
			// handle invalid token
			if (requestInitializer.getAuthToken() == null) {
				accountManager.manager.getAuthToken(account, AUTH_TOKEN_TYPE,
						true, new AccountManagerCallback<Bundle>() {

							public void run(AccountManagerFuture<Bundle> future) {
								try {
									Bundle bundle = future.getResult();
									if (bundle
											.containsKey(AccountManager.KEY_INTENT)) {
										Intent intent = bundle
												.getParcelable(AccountManager.KEY_INTENT);
										int flags = intent.getFlags();
										flags &= ~Intent.FLAG_ACTIVITY_NEW_TASK;
										intent.setFlags(flags);
										startActivityForResult(intent,
												REQUEST_AUTHENTICATE);
									} else if (bundle
											.containsKey(AccountManager.KEY_AUTHTOKEN)) {
										setAuthToken(bundle
												.getString(AccountManager.KEY_AUTHTOKEN));
										listAllProfiles();
									}
								} catch (Exception e) {
									handleException(e);
								}
							}
						}, null);
			} else {
				try {
					listAllProfiles();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			return;
		}
		chooseAccount();
	}

	private void chooseAccount() {
		accountManager.manager.getAuthTokenByFeatures(
				GoogleAccountManager.ACCOUNT_TYPE, AUTH_TOKEN_TYPE, null,
				ProfilesActivity.this, null, null,
				new AccountManagerCallback<Bundle>() {

					public void run(AccountManagerFuture<Bundle> future) {
						Bundle bundle;
						try {
							bundle = future.getResult();
							setAccountName(bundle
									.getString(AccountManager.KEY_ACCOUNT_NAME));
							setAuthToken(bundle
									.getString(AccountManager.KEY_AUTHTOKEN));
							listAllProfiles();
						} catch (OperationCanceledException e) {
							// user canceled
						} catch (AuthenticatorException e) {
							handleException(e);
						} catch (IOException e) {
							handleException(e);
						}
					}
				}, null);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
		case REQUEST_AUTHENTICATE:
			if (resultCode == RESULT_OK) {
				gotAccount();
			} else {
				chooseAccount();
			}
			break;
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if (accountManager.getAccounts().length >= 2) {
			menu.add(0, MENU_ACCOUNTS, 0, getString(R.string.switch_account));
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_ADD:
			return true;
		case MENU_ACCOUNTS:
			chooseAccount();
			return true;
		}
		return false;
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		menu.add(0, CONTEXT_EDIT, 0, getString(R.string.update_title));
		menu.add(0, CONTEXT_DELETE, 0, getString(R.string.delete));
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
				.getMenuInfo();

		// CalendarEntry calendar = calendars.get((int) info.id);
		// try {
		// switch (item.getItemId()) {
		// case CONTEXT_EDIT:
		// CalendarEntry patchedCalendar = calendar.clone();
		// patchedCalendar.title = calendar.title + " UPDATED " + new
		// DateTime(new Date());
		// client.executePatchRelativeToOriginal(calendar, patchedCalendar);
		// executeRefreshCalendars();
		// return true;
		// case CONTEXT_DELETE:
		// client.executeDelete(calendar);
		// executeRefreshCalendars();
		// return true;
		// default:
		// return super.onContextItemSelected(item);
		// }
		// } catch (IOException e) {
		// handleException(e);
		// }
		return false;
	}

	void listAllProfiles() throws IOException {

		profiles = client.management().profiles().list("~all", "~all")
				.execute().getItems();

		List<String> names = Lists.transform(profiles,
				new Function<Profile, String>() {
					@Override
					public String apply(Profile profile) {
						return profile.getName();
					}
				});

		getListView().setAdapter(
				new ArrayAdapter<String>(this,
						android.R.layout.simple_list_item_1, names));
	}

	void handleException(Exception e) {
		e.printStackTrace();
		if (e instanceof HttpResponseException) {
			HttpResponse response = ((HttpResponseException) e).getResponse();
			int statusCode = response.getStatusCode();
			try {
				response.ignore();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			// TODO(yanivi): should only try this once to avoid infinite loop
			if (statusCode == 401) {
				gotAccount();
				return;
			}
			try {
				Log.e(TAG, response.parseAsString());
			} catch (IOException parseException) {
				parseException.printStackTrace();
			}
		}
		Log.e(TAG, e.getMessage(), e);
	}

}