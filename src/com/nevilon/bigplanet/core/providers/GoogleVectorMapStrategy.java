package com.nevilon.bigplanet.core.providers;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

public class GoogleVectorMapStrategy extends MapStrategy {

	private List<Layer> layers = new ArrayList<Layer>();

    public GoogleVectorMapStrategy() {
		layers.add(new Layer() {

			private  String SERVER = "http://mt.google.com/"; 
			
			@Override
			public String getDescription() {
				return null;
			}

			@Override
			public int getId() {
				return 0;
			}
			
			@Override
			public String getURLPattern() {
				//return SERVER+ "mt/v=w2.92&hl=en&x={0}&y={1}&z={2}&s=Galil";
				return SERVER+ "vt/v=w2.999&hl=zh-TW&x={0}&y={1}&z={2}&s=";
			}


		});
		
		


	}
	
	@Override
	public String getURL(int x, int y, int z,int layout) {
		Layer layer = layers.get(layout);
		String tmp = MessageFormat.format(layer.getURLPattern(),
				String.valueOf(x), String.valueOf(y), String.valueOf(17-z));
 
		return tmp;
	}

	@Override
	public String getDescription() {
		return "Google Map";
	}

}
