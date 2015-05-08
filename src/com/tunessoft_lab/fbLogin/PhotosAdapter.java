package com.tunessoft_lab.fbLogin;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

public class PhotosAdapter extends BaseAdapter {

    private Activity activity;

    ArrayList<getPhotos> arrayPhotos;

    private static LayoutInflater inflater = null;
    ImageLoader imageLoader; 

    public PhotosAdapter(Activity a, ArrayList<getPhotos> arrPhotos) {

        activity = a;

        arrayPhotos = arrPhotos;

        inflater = (LayoutInflater)activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        imageLoader = new ImageLoader(activity.getApplicationContext());
    }

    @Override
	public int getCount() {
        return arrayPhotos.size();
    }

    @Override
	public Object getItem(int position) {
        return arrayPhotos.get(position);
    }

    @Override
	public long getItemId(int position) {
        return position;
    }

    @Override
	public View getView(final int position, View convertView, ViewGroup parent) {

        ViewHolder holder;

        View vi = convertView;
        if(convertView == null) {
            vi = inflater.inflate(R.layout.photo_items, null);

            holder = new ViewHolder();

            holder.imgPhoto = (ImageView)vi.findViewById(R.id.image);

            vi.setTag(holder);
          } else {
            holder = (ViewHolder) vi.getTag();
        }

        if (arrayPhotos.get(position).getPhotoPicture() != null){
            imageLoader.DisplayImage(arrayPhotos.get(position).getPhotoPicture(), holder.imgPhoto);
            Log.d("PhotoAdapter", "Inside PhotoAdapter");
        }
        return vi;
    }

    static class ViewHolder {
        ImageView imgPhoto;

    }
}