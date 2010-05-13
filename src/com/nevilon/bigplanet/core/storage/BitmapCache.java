package com.nevilon.bigplanet.core.storage;

import android.graphics.Bitmap;

import com.nevilon.bigplanet.core.RawTile;

/**
 * Кеш битмапов
 * 
 * @author hudvin
 * 
 */
public class BitmapCache {

	private ExpiredHashMap cacheMap;

	/**
	 * Конструктор
	 * 
	 * @param size
	 *            размер кеша
	 */
	public BitmapCache(int size) {
		cacheMap = new ExpiredHashMap(size);
	}


	public void clear(){
		cacheMap.clear();
	}
	
	public void gc(){
		cacheMap.gc();
	}
	
	/**
	 * Добавление битмапа в кеш
	 * 
	 * @param tile
	 *            тайл
	 * @param bitmap
	 *            битмап
	 */
	public void put(RawTile tile, Bitmap bitmap) {
		if(tile.s==-1){
			throw new IllegalStateException();
		}
		cacheMap.put(tile, bitmap);
	}

	/**
	 * Получение битмапа из кеша
	 * 
	 * @param tile
	 *            тайл
	 * @return битмап (или null если не найден)
	 */
	public Bitmap get(RawTile tile) {
		return cacheMap.get(tile);
	}


}
