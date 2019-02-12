package com.example.kienphung.lesson5;

import android.net.Uri;

public class Song {
    private Uri mUri;
    private String mTitle;
    private String mArtist;
    private byte[] mPicture;

    public Song(Uri uri, String title, String artist, byte[] picture) {
        mUri = uri;
        mTitle = title;
        mArtist = artist;
        mPicture = picture;
    }

    public Uri getUri() {
        return mUri;
    }

    public void setUri(Uri uri) {
        mUri = uri;
    }

    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String title) {
        mTitle = title;
    }

    public String getArtist() {
        return mArtist;
    }

    public void setArtist(String artist) {
        mArtist = artist;
    }

    public byte[] getPicture() {
        return mPicture;
    }

    public void setPicture(byte[] picture) {
        mPicture = picture;
    }
}
