package com.tunessoft_lab.fbLogin;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

public class OnGridImageClick extends Activity {

	ImageView iv;
	ProgressBar pb;
	String imageUrl;
	Bitmap b=null;
	Drawable d;
	getPhotoFromGrid gd;
	
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.gridview_image_selected);
		iv=(ImageView)findViewById(R.id.loadedimage);
		pb=(ProgressBar)findViewById(R.id.after_grid_progress);
		
		Intent i=getIntent();
		imageUrl=i.getStringExtra("IMAGE_URL");
		gd=new getPhotoFromGrid();
		gd.execute();
	}
	

	private class getPhotoFromGrid extends AsyncTask<Void, Void, Void>
	{
		
		protected void onPreExecute()
		{
		}
		
		@Override
		protected Void doInBackground(Void... params) {
			
			try {
				URL imageURL=new URL(imageUrl);
				HttpURLConnection conn=(HttpURLConnection)imageURL.openConnection();
				Log.i("After GridView", "Opening Connection...");
				InputStream is=conn.getInputStream();
				 d=Drawable.createFromStream(is, "src");
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (Throwable e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			
			return null;
			// TODO Auto-generated method stub
		}
		
		protected void onPostExecute(Void result)
		{
			pb.setVisibility(View.GONE);
			iv.setImageDrawable(d);
		}
	}
}
	
