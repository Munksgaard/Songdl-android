package com.munksgaard.songdl.model;

import java.net.URL;

public class SongDetails {
	private String title;
	private String artist;
	private String duration;
	private URL url;
	
	public SongDetails() {
		// pass
	}
	
	public String getTitle() {
		return title;
	}
	
	public void setTitle(String title) {
		this.title = title;
	}
	
	public String getDuration() {
		return duration;
	}
	
	public void setDuration(String secs) {
		Integer decoded_secs = Integer.decode(secs);
		Integer mins = decoded_secs / 60;
		Integer sec = decoded_secs - 60 * mins;
		String sec_string = sec.toString();
		if (sec_string.length() == 1) {
			sec_string = "0" + sec_string;
		}
		String dur = mins.toString() + ":" + sec_string;
		this.duration = dur;
	}
	
	public String getArtist() {
		return artist;
	}
	
	public void setArtist(String artist) {
		this.artist = artist;
	}
	
	public URL getUrl() {
		return url;
	}
	
	public void setUrl(URL url) {
		this.url = url;
	}
	
	public String toString() {
		return artist + " - " + title + " (" + duration + ")";
	}
	
	public String toFilename() {
		return artist + " - " + title + ".mp3";
	}
}
