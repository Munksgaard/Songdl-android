package com.munksgaard.songdl;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.content.Context;


import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.munksgaard.songdl.model.SongDetails;
import com.munksgaard.songdl.util.MD5;

public class MainActivity extends ListActivity {

	private static final String TAG = "MainActivity";

	private ArrayList<SongDetails> _songDetailList = null;
	private ArrayAdapter<SongDetails> _songDetailListAdapter = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_layout);

		_songDetailList = new ArrayList<SongDetails>();
		_songDetailListAdapter = new ArrayAdapter<SongDetails>(this, android.R.layout.simple_list_item_1, _songDetailList);
		setListAdapter(_songDetailListAdapter);

		EditText editText = (EditText) findViewById(R.id.search);
		editText.setOnEditorActionListener(new OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				boolean handled = false;
				if (actionId == EditorInfo.IME_ACTION_SEARCH) {
					EditText editText = (EditText) findViewById(R.id.search);
					InputMethodManager imm = (InputMethodManager)getSystemService(
							Context.INPUT_METHOD_SERVICE);
					imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);

					String query_string = v.getText().toString();
					URL url = queryURL(query_string);

					new FetchQueryResults().execute(url);
					handled = true;
				}
				return handled;
			}
		});


	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main_layout, menu);
		return true;
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {		
		SongDetails element = _songDetailList.get(position);

		new DownloadSong().execute(element);
	}

	private URL queryURL(String query_string) {
		String url_encoded_query = null;
		try {
			url_encoded_query = URLEncoder.encode(query_string, "utf-8");
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}
		String query = "327488api_id=525159count=10format=jsonmethod=audio.searchq=" + query_string + "test_mode=1g5vuj9EWFO";

		MD5 md5 = new MD5();
		try {
			md5.Update(query, null);
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String hashed_query = md5.asHex();

		Log.d(TAG, "hashed_query = " + hashed_query);

		URL url = null;
		try {
			url = new URL("http://api.vk.com/api.php?api_id=525159&method=audio.search&format=json&sig=" + hashed_query + "&test_mode=1&count=10&q=" + url_encoded_query);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}

		Log.d(TAG, "url = " + url.toString());

		return url;
	}

	private class DownloadSong extends AsyncTask<SongDetails, Integer, String> {
		ProgressDialog progressDialog = null;

		@Override
		protected void onPreExecute() {
			super.onPreExecute();

			progressDialog = new ProgressDialog(MainActivity.this);

			progressDialog.setCancelable(false);
			progressDialog.setMessage("Getting song results ...");
			progressDialog.setTitle("Please wait");
			progressDialog.setIndeterminate(true);
			progressDialog.setMax(100);
			progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			progressDialog.show();
			publishProgress(96);
		}

		@Override
		protected String doInBackground(SongDetails... songs) {
			try {
				SongDetails song = songs[0];
				URL url = song.getUrl();
				URLConnection connection = url.openConnection();
				connection.connect();
				Log.d(TAG, "trying do download song " + url.toString());

				// this will be useful so that you can show a typical 0-100% progress bar
				int fileLength = connection.getContentLength();
				Log.d(TAG, "length " + fileLength);
				// download the file
				InputStream input = new BufferedInputStream(url.openStream());

				OutputStream output = new FileOutputStream("/mnt/sdcard/Music/" + song.toFilename());
				Log.d(TAG, "hej?");

				byte data[] = new byte[1024];
				long total = 0;
				int count;
				while ((count = input.read(data)) != -1) {
					total += count;
					// publishing the progress....
					publishProgress((int) (total * 100 / fileLength));
					output.write(data, 0, count);
				}

				Log.d(TAG, "Downloaded bytes " + total);

				output.flush();
				output.close();
				input.close();
			} catch (Exception e) {
			}

			return null;
		}

		@Override
		protected void onProgressUpdate(Integer... progress) {
			super.onProgressUpdate(progress);
			Log.d(TAG, "setting progress " + progress[0].toString());
			progressDialog.setProgress(progress[0]);
		}

		@Override
		protected void onPostExecute(String result) {
			if (progressDialog != null && progressDialog.isShowing()) {
				progressDialog.dismiss();
			}
		}
	}

	private class FetchQueryResults extends AsyncTask<URL, Void, String> {

		ProgressDialog progressDialog = new ProgressDialog(MainActivity.this);

		@Override
		protected void onPreExecute() {
			progressDialog.setCancelable(false);
			progressDialog.setMessage("Getting song results ...");
			progressDialog.setTitle("Please wait");
			progressDialog.setIndeterminate(true);
			progressDialog.show();

			_songDetailList.clear();
			_songDetailListAdapter.notifyDataSetChanged();
		}

		@Override
		protected String doInBackground(URL... urls) {
			if (isOnline()) {
				try {
					Log.d(TAG, "Getting results from url " + urls[0].toString());
					JsonObject results = readSongsJson(urls[0]);
					JsonArray array = results.get("response").getAsJsonArray(); 
					Log.d(TAG, "array1  " + array.toString());
					for (int i = 1; i < array.size(); i++) {
						JsonObject obj = array.get(i).getAsJsonObject().get("audio").getAsJsonObject();
						Log.d(TAG, "obj " + obj.toString());
						SongDetails song = new SongDetails();
						song.setArtist(obj.get("artist").getAsString());
						song.setTitle(obj.get("title").getAsString());
						song.setUrl(new URL(obj.get("url").getAsString().replaceAll("\\/", "/")));
						song.setDuration(obj.get("duration").getAsString());
						_songDetailList.add(song);
					}

				} catch (IOException e) {

					Log.e(TAG, e.toString());
				} catch (Exception e) {
					Log.e(TAG, e.toString());
				}

			} else {
				//Toast..toastOnUiThread(MainActivity.this, "No internet connection...");
			}
			Log.d(TAG, _songDetailList.toString());
			return null;
		}

		@Override
		protected void onPostExecute(String result) {
			_songDetailListAdapter.notifyDataSetChanged();
			if (progressDialog != null && progressDialog.isShowing()) {
				progressDialog.dismiss();
			}

			Log.d(TAG, "Fetched results = " + _songDetailList.size());
		}

		public JsonObject readSongsJson(URL url) {
			StringBuilder builder = new StringBuilder();
			HttpClient client = new DefaultHttpClient();
			HttpGet httpGet = null;
			try {
				httpGet = new HttpGet(url.toURI());
			} catch (URISyntaxException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			try {
				HttpResponse response = client.execute(httpGet);
				StatusLine statusLine = response.getStatusLine();
				int statusCode = statusLine.getStatusCode();
				if (statusCode == 200) {
					HttpEntity entity = response.getEntity();
					InputStream content = entity.getContent();
					BufferedReader reader = new BufferedReader(new InputStreamReader(content));
					String line;
					while ((line = reader.readLine()) != null) {
						builder.append(line);
					}
				} else {
					Log.e(TAG, "Failed to download file");
				}
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			String str = builder.toString();
			JsonElement elem = new JsonParser().parse(str);
			JsonObject obj = elem.getAsJsonObject();
			Log.d(TAG, obj.toString());
			return obj;
		}
	}

	/**
	 * check if we're able to get online
	 * 
	 * @return boolean
	 */
	public boolean isOnline() {
		ConnectivityManager conMgr = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);

		Boolean connected = (   conMgr.getActiveNetworkInfo() != null &&
				conMgr.getActiveNetworkInfo().isAvailable() &&
				conMgr.getActiveNetworkInfo().isConnected());
		if (connected) {
			Log.d(TAG, "is online");
			return true;
		} else {
			return false;
		}
	}

}
