package com.nevilon.bigplanet.core.providers;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

public class GoogleLandscapeMapStrategy extends MapStrategy {

	private List<Layer> layers = new ArrayList<Layer>();

	public GoogleLandscapeMapStrategy() {
		layers.add(new Layer() {

			private String SERVER = "http://mt1.google.com/"; 
			
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
				return SERVER+"vt/v=w2p.106&hl=zh-TW&x={0}&y={1}&z={2}&s=Galil";
			}


		});

	}


	@Override
	public String getURL(int x, int y, int z, int layout) {
		Layer layer = layers.get(layout);
		return MessageFormat.format(layer.getURLPattern(),
				String.valueOf(x), String.valueOf(y), String.valueOf(17 - z));
	}

	@Override
	public String getDescription() {
		return "Google Landscape";
	}

}
