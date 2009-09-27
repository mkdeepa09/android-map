package com.nevilon.bigplanet.core.providers;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;


public class YandexVectorMapStrategy extends MapStrategy {
	
	private List<Layer> layers = new ArrayList<Layer>();
	
	public YandexVectorMapStrategy(){
		layers.add(new Layer(){

			private  String SERVER = "http://vec02.maps.yandex.ru/";
			
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
				return SERVER+"tiles?l=map&v=2.4.2&x={0}&y={1}&z={2}";
			}

			
		});
		
		
	}
	
	

	@Override
	public String getURL(int x, int y, int z, int layout) {
		Layer layer = layers.get(layout);
		String url = MessageFormat.format(layer.getURLPattern(),
				String.valueOf(x), String.valueOf(y), String.valueOf(17-z)); 
		return url;
	}

	@Override
	public String getDescription() {
		return "Yandex Map";
	}


}
