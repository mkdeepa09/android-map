package com.nevilon.bigplanet.core.providers;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

public class GoogleSatelliteMapStrategy extends MapStrategy {
	private List<Layer> layers = new ArrayList<Layer>();

	public GoogleSatelliteMapStrategy() {
		layers.add(new Layer() {

			private String SERVER = "http://khm0.google.com/"; 
			
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
				return SERVER+"kh/v=45&hl=zh-TW&x={0}&y={1}&z={2}&s=Galile";
			}

			

		});

	}


	@Override
	public String getURL(int x, int y, int z, int layout) {
		Layer layer = layers.get(layout);
		return MessageFormat.format(layer.getURLPattern(),
				String.valueOf(x), String.valueOf(y), String.valueOf(17-z));
	}

	@Override
	public String getDescription() {
		return "Google Satellite";
	}

}
