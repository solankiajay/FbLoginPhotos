/**
 * Copyright (c) 2014-present, Facebook, Inc. All rights reserved.
 *
 * You are hereby granted a non-exclusive, worldwide, royalty-free license to use,
 * copy, modify, and distribute this software in source code or binary form for use
 * in connection with the web services and APIs provided by Facebook.
 *
 * As with any software that integrates with the Facebook platform, your use of
 * this software is subject to the Facebook Developer Principles and Policies
 * [http://developers.facebook.com/policy/]. This copyright notice shall be
 * included in all copies or substantial portions of the software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tunessoft_lab.fbLogin;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookAuthorizationException;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.Profile;
import com.facebook.ProfileTracker;
import com.facebook.appevents.AppEventsLogger;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.ProfilePictureView;
import com.facebook.share.ShareApi;
import com.facebook.share.Sharer;
import com.facebook.share.model.ShareLinkContent;
import com.facebook.share.model.SharePhoto;
import com.facebook.share.model.SharePhotoContent;
import com.facebook.share.widget.ShareDialog;

public class FBLoginActivity extends FragmentActivity {

	String[] permissions = { "offline_access", "publish_stream", "user_photos",
			"publish_checkins", "photo_upload" };
	String PHOTO_PERMISSION = "user_photos";
	private static final String PERMISSION = "publish_actions";
	private static final Location SEATTLE_LOCATION = new Location("") {
		{
			setLatitude(47.6097);
			setLongitude(-122.3331);
		}
	};

	private final String PENDING_ACTION_BUNDLE_KEY = "com.facebook.samples.hellofacebook:PendingAction";

	private ProfilePictureView profilePictureView;
	private TextView greeting;
	private PendingAction pendingAction = PendingAction.NONE;
	private boolean canPresentShareDialog;
	private boolean canPresentShareDialogWithPhotos;
	private CallbackManager callbackManager;
	private ProfileTracker profileTracker;
	private ShareDialog shareDialog;

	URL fb;
	GridView gridOfPhotos;
	ProgressBar spin, spin2;
	Handler handler;

	// HOLD THE URL TO MAKE THE API CALL TO
	private String URL, at;

	// STORE THE PAGING URL
	private String pagingURL;

	// FLAG FOR CURRENT PAGE
	int current_page = 1, c = 30;

	// BOOLEAN TO CHECK IF NEW FEEDS ARE LOADING
	Boolean loadingMore = true;

	Boolean stopLoadingData = false;

	String FILENAME = "AndroidSSO_data";
	SharedPreferences mPrefs;
	SharedPreferences.Editor editor;
	PhotosAdapter adapter;
	ArrayList<getPhotos> arrPhotos;
	Context mContext;
	ProgressDialog progressDialog;
	Button loadPhotos, loadMorePhotos;

	private FacebookCallback<Sharer.Result> shareCallback = new FacebookCallback<Sharer.Result>() {
		@Override
		public void onCancel() {
			Log.d("HelloFacebook", "Canceled");
		}

		@Override
		public void onError(FacebookException error) {
			Log.d("HelloFacebook", String.format("Error: %s", error.toString()));
			String title = getString(R.string.error);
			String alertMessage = error.getMessage();
			showResult(title, alertMessage);
		}

		@Override
		public void onSuccess(Sharer.Result result) {
			Log.d("HelloFacebook", "Success!");
			if (result.getPostId() != null) {
				String title = getString(R.string.success);
				String id = result.getPostId();
				String alertMessage = getString(
						R.string.successfully_posted_post, id);
				showResult(title, alertMessage);
			}
		}

		private void showResult(String title, String alertMessage) {
			new AlertDialog.Builder(FBLoginActivity.this)
					.setTitle(title).setMessage(alertMessage)
					.setPositiveButton(R.string.ok, null).show();
		}
	};

	private enum PendingAction {
		NONE, POST_PHOTO, POST_STATUS_UPDATE
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		FacebookSdk.sdkInitialize(this.getApplicationContext());

		callbackManager = CallbackManager.Factory.create();

		LoginManager.getInstance().registerCallback(callbackManager,
				new FacebookCallback<LoginResult>() {

					@Override
					public void onSuccess(LoginResult loginResult) {
						
						updateUI();

						mPrefs = getApplicationContext().getSharedPreferences("MyPref", MODE_PRIVATE); 
						editor = mPrefs.edit();
						editor.putString("access_token", loginResult
								.getAccessToken().getToken());
						editor.putString("user_id", loginResult
								.getAccessToken().getUserId());
						editor.commit();

					}

					@Override
					public void onCancel() {
						if (pendingAction != PendingAction.NONE) {
							showAlert();
							pendingAction = PendingAction.NONE;
						}
						updateUI();
					}

					@Override
					public void onError(FacebookException exception) {
						if (pendingAction != PendingAction.NONE
								&& exception instanceof FacebookAuthorizationException) {
							showAlert();
							pendingAction = PendingAction.NONE;
						}
						updateUI();
					}

					private void showAlert() {
						new AlertDialog.Builder(
								FBLoginActivity.this)
								.setTitle(R.string.cancelled)
								.setMessage(R.string.permission_not_granted)
								.setPositiveButton(R.string.ok, null).show();
					}

				});

		shareDialog = new ShareDialog(this);
		shareDialog.registerCallback(callbackManager, shareCallback);

		if (savedInstanceState != null) {
			String name = savedInstanceState
					.getString(PENDING_ACTION_BUNDLE_KEY);
			pendingAction = PendingAction.valueOf(name);
		}

		setContentView(R.layout.main);

		gridOfPhotos = (GridView) findViewById(R.id.gridphoto);
		spin = (ProgressBar) findViewById(R.id.load);
		spin2 = (ProgressBar) findViewById(R.id.load2);
		loadPhotos = (Button) findViewById(R.id.btn_loadPhoto);
		loadMorePhotos = (Button) findViewById(R.id.btn_loadMorePhoto);

		handler = new Handler();

		mContext = this;

		arrPhotos = new ArrayList<getPhotos>();

		profileTracker = new ProfileTracker() {
			@Override
			protected void onCurrentProfileChanged(Profile oldProfile,
					Profile currentProfile) {
				updateUI();
			}
		};

		loadPhotos.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {

				mPrefs = getSharedPreferences("MyPref", MODE_PRIVATE);
				String access_token = mPrefs.getString("access_token", null);

				if (access_token != null) {

					if (hasPhotoPermission()) {

						getPhotosData gd = new getPhotosData();
						gd.execute();

						loadMorePhotos.setVisibility(View.VISIBLE);
					} else {

						LoginManager.getInstance().logInWithReadPermissions(
								FBLoginActivity.this,
								Arrays.asList(PHOTO_PERMISSION));

					}

				}

			}
		});

		loadMorePhotos.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {

				mPrefs = getSharedPreferences("MyPref", MODE_PRIVATE);
				String access_token = mPrefs.getString("access_token", null);

				if (access_token != null) {

					if (hasPhotoPermission()) {

						loadMorePhotos ld = new loadMorePhotos();
						ld.execute();
					}
				} else {

					LoginManager.getInstance().logInWithReadPermissions(
							FBLoginActivity.this,
							Arrays.asList(PHOTO_PERMISSION));

				}

			}
		});

		profilePictureView = (ProfilePictureView) findViewById(R.id.profilePicture);
		greeting = (TextView) findViewById(R.id.greeting);

		canPresentShareDialog = ShareDialog.canShow(ShareLinkContent.class);

		canPresentShareDialogWithPhotos = ShareDialog
				.canShow(SharePhotoContent.class);

		gridOfPhotos.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View v,
					int position, long id) {

				PhotosAdapter pa = (PhotosAdapter) parent.getAdapter();
				getPhotos gp = (getPhotos) pa.getItem(position);

				String name = gp.getPhotoSource();
				try {
					if (!name.isEmpty()) {
						Intent i = new Intent(getApplicationContext(),
								OnGridImageClick.class);
						i.putExtra("IMAGE_URL", name);
						startActivity(i);
					}

				} catch (Exception e) {
					Log.e("GridView", e.getMessage());
				}
			}
		});

		EndlessScrollListener s = new EndlessScrollListener(gridOfPhotos);
		gridOfPhotos.setOnScrollListener(s);

	}

	public class EndlessScrollListener implements OnScrollListener {

		private GridView gridView;

		public EndlessScrollListener(GridView gridView) {
			this.gridView = gridView;
		}

		public void onScroll(AbsListView view, int firstVisibleItem,
				int visibleItemCount, int totalItemCount) {
			if (gridView.getLastVisiblePosition() + 1 == totalItemCount) {
				Log.i("GRID_SCROLL", "firstVisible " + firstVisibleItem
						+ "visibleItemCount " + visibleItemCount
						+ "totalItemCount " + totalItemCount);
				// loadMorePhotos ld=new loadMorePhotos();
				// ld.execute();
			}
		}

		public void onScrollStateChanged(AbsListView view, int scrollState) {

		}
	}

	@Override
	protected void onResume() {
		super.onResume();

		// Call the 'activateApp' method to log an app event for use in
		// analytics and advertising
		// reporting. Do so in the onResume methods of the primary Activities
		// that an app may be
		// launched into.
		AppEventsLogger.activateApp(this);

		updateUI();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		outState.putString(PENDING_ACTION_BUNDLE_KEY, pendingAction.name());
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		callbackManager.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	public void onPause() {
		super.onPause();

		// Call the 'deactivateApp' method to log an app event for use in
		// analytics and advertising
		// reporting. Do so in the onPause methods of the primary Activities
		// that an app may be
		// launched into.
		AppEventsLogger.deactivateApp(this);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		profileTracker.stopTracking();
	}

	private void updateUI() {
		boolean enableButtons = AccessToken.getCurrentAccessToken() != null;

		Profile profile = Profile.getCurrentProfile();
		if (enableButtons && profile != null) {
			profilePictureView.setProfileId(profile.getId());
			greeting.setText(getString(R.string.hello_user,
					profile.getFirstName()));

			loadPhotos.setVisibility(View.VISIBLE);

		} else {
			profilePictureView.setProfileId(null);
			greeting.setText(null);
			loadPhotos.setVisibility(View.GONE);
			loadMorePhotos.setVisibility(View.GONE);
			gridOfPhotos.setAdapter(null);
		}

	}

	private boolean hasPhotoPermission() {
		AccessToken accessToken = AccessToken.getCurrentAccessToken();
		return accessToken != null
				&& accessToken.getPermissions().contains("user_photos");
	}

	private class getPhotosData extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... arg0) {

			loadingMore = true;

			// SET THE INITIAL URL TO GET THE FIRST LOT OF ALBUMS
			URL = "https://graph.facebook.com/"
					+ mPrefs.getString("user_id", null)
					+ "/photos/?access_token="
					+ mPrefs.getString("access_token", null) + "&limit=20";
			at = mPrefs.getString("access_token", null);

			String nameURL = "https://graph.facebook.com/me/?access_token="
					+ at + "&fields=name";

			try {

				HttpClient hc = new DefaultHttpClient();
				HttpGet get = new HttpGet(URL);
				HttpResponse rp = hc.execute(get);

				String st = rp.toString();

				Log.e("RESPONSE", st);
				Log.e("ACCESS_TOKEN", at);

				if (rp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
					String queryAlbums = EntityUtils.toString(rp.getEntity());

					Log.d("GRAPH-RESPONSE", queryAlbums);

					int s = rp.getStatusLine().getStatusCode();
					String a = String.valueOf(s);
					Log.e("RESPONSE-IF", a);

					JSONObject JOTemp = new JSONObject(queryAlbums);

					JSONArray JAPhotos = JOTemp.getJSONArray("data");

					Log.e("JSONArray", String.valueOf(JAPhotos));
					Log.e("JSONArray-Length", String.valueOf(JAPhotos.length()));
					// IN MY CODE, I GET THE NEXT PAGE LINK HERE

					getPhotos photos;

					for (int i = 0; i < JAPhotos.length(); i++) {
						JSONObject JOPhotos = JAPhotos.getJSONObject(i);
						// Log.e("INDIVIDUAL ALBUMS", JOPhotos.toString());

						Log.e("JSON", String.valueOf(i));

						if (JOPhotos.has("link")) {

							photos = new getPhotos();

							// GET THE ALBUM ID
							if (JOPhotos.has("id")) {
								photos.setPhotoID(JOPhotos.getString("id"));
							} else {
								photos.setPhotoID(null);
							}

							// GET THE ALBUM NAME
							if (JOPhotos.has("name")) {
								photos.setPhotoName(JOPhotos.getString("name"));
							} else {
								photos.setPhotoName(null);
							}

							// GET THE ALBUM COVER PHOTO
							if (JOPhotos.has("picture")) {
								photos.setPhotoPicture(JOPhotos
										.getString("picture"));
							} else {
								photos.setPhotoPicture(null);
							}

							// GET THE PHOTO'S SOURCE
							if (JOPhotos.has("source")) {
								photos.setPhotoSource(JOPhotos
										.getString("source"));
							} else {
								photos.setPhotoSource(null);
							}

							arrPhotos.add(photos);
						}
					}
				} else {
					int s = rp.getStatusLine().getStatusCode();
					String a = String.valueOf(s);
					Log.e("RESPONSE-Else", a);
				}

			} catch (Exception e) {
				e.printStackTrace();
			}

			Log.e("doInBackground Finished", "after doInBackground");
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {

			adapter = new PhotosAdapter(FBLoginActivity.this,
					arrPhotos);
			Log.e("onPostExceute", "inside PostExecute");
			// SET THE ADAPTER TO THE GRIDVIEW
			gridOfPhotos.setAdapter(adapter);

			// Toast.makeText(getApplicationContext(), "on Post Excecute",
			// Toast.LENGTH_SHORT).show();
			Log.e("onPostExceute", "");
			spin.setVisibility(View.GONE);

			// CHANGE THE LOADING MORE STATUS
			loadingMore = false;
			Log.e("onPostExceute", "end of PostExecute");
		}

		@Override
		protected void onPreExecute() {
			spin.setVisibility(View.VISIBLE);
		}

	}

	private class loadMorePhotos extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... arg0) {

			// SET LOADING MORE "TRUE"
			loadingMore = true;

			// INCREMENT CURRENT PAGE
			current_page += 1;

			// Next page request
			URL = "https://graph.facebook.com/"
					+ mPrefs.getString("user_id", null)
					+ "/photos/?access_token="
					+ mPrefs.getString("access_token", null)
					+ "&limit=30&offset=" + c;
			c += 30;
			try {

				HttpClient hc = new DefaultHttpClient();
				HttpGet get = new HttpGet(URL);
				HttpResponse rp = hc.execute(get);

				if (rp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
					String queryAlbums = EntityUtils.toString(rp.getEntity());
					// Log.e("PAGED RESULT", queryAlbums);

					JSONObject JOTemp = new JSONObject(queryAlbums);

					JSONArray JAPhotos = JOTemp.getJSONArray("data");

					// IN MY CODE, I GET THE NEXT PAGE LINK HERE

					getPhotos photos;

					for (int i = 0; i < JAPhotos.length(); i++) {
						JSONObject JOPhotos = JAPhotos.getJSONObject(i);
						// Log.e("INDIVIDUAL ALBUMS", JOPhotos.toString());

						if (JOPhotos.has("link")) {

							photos = new getPhotos();

							// GET THE ALBUM ID
							if (JOPhotos.has("id")) {
								photos.setPhotoID(JOPhotos.getString("id"));
							} else {
								photos.setPhotoID(null);
							}

							// GET THE ALBUM NAME
							if (JOPhotos.has("name")) {
								photos.setPhotoName(JOPhotos.getString("name"));
							} else {
								photos.setPhotoName(null);
							}

							// GET THE ALBUM COVER PHOTO
							if (JOPhotos.has("picture")) {
								photos.setPhotoPicture(JOPhotos
										.getString("picture"));
							} else {
								photos.setPhotoPicture(null);
							}

							// GET THE ALBUM'S PHOTO COUNT
							if (JOPhotos.has("source")) {
								photos.setPhotoSource(JOPhotos
										.getString("source"));
							} else {
								photos.setPhotoSource(null);
							}

							arrPhotos.add(photos);
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

			return null;
		}

		@Override
		protected void onPostExecute(Void result) {

			// get listview current position - used to maintain scroll position
			int currentPosition = gridOfPhotos.getFirstVisiblePosition();

			// APPEND NEW DATA TO THE ARRAYLIST AND SET THE ADAPTER TO THE
			// LISTVIEW
			adapter = new PhotosAdapter(FBLoginActivity.this,
					arrPhotos);
			gridOfPhotos.setAdapter(adapter);

			spin2.setVisibility(View.GONE);
			// Setting new scroll position
			gridOfPhotos.setSelection(currentPosition + 1);

			// SET LOADINGMORE "FALSE" AFTER ADDING NEW FEEDS TO THE EXISTING
			// LIST
			loadingMore = false;
		}

		protected void onPreExecute() {
			spin2.setVisibility(View.VISIBLE);
		}

	}
}
