package com.tunessoft_lab.fbLogin;

import android.util.Log;

public class getPhotos {

    String PhotoID;

    String PhotoName;

    String PhotoPicture;

    String PhotoSource;

    // SET THE PHOTO ID
    public void setPhotoID(String PhotoID)  {
        this.PhotoID = PhotoID;
    }

    // GET THE PHOTO ID
    public String getPhotoID()  {
        return PhotoID;
    }

    // SET THE PHOTO NAME
    public void setPhotoName(String PhotoName)  {
        this.PhotoName = PhotoName;
    }

    // GET THE PHOTO NAME
    public String getPhotoName()    {
    	Log.e("NAME", PhotoName);
    	return PhotoName;
    }

    // SET THE PHOTO PICTURE
    public void setPhotoPicture(String PhotoPicture)    {
        this.PhotoPicture = PhotoPicture;
    }

    // GET THE PHOTO PICTURE
    public String getPhotoPicture() {
        return PhotoPicture;
    }

    // SET THE PHOTO SOURCE
    public void setPhotoSource(String PhotoSource)  {
        this.PhotoSource = PhotoSource;
    }

    // GET THE PHOTO SOURCE
    public String getPhotoSource()  {
        return PhotoSource;
    }
}