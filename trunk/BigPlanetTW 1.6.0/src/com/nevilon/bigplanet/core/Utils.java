package com.nevilon.bigplanet.core;

public class Utils {

	public static int getZoomLevel(double x) {
		if(x<1) x = 1/x;
		int counter = 1;
		while (x > 2) {
			counter++;
			x = x / 2;
		}
		if(x<1) return counter*-1;
		return counter;
	}

}
