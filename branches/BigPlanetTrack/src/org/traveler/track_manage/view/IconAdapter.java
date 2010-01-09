package org.traveler.track_manage.view;

import java.util.ArrayList;

import com.nevilon.bigplanet.R;


import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class IconAdapter extends BaseAdapter {
	
	private Context _ctx;
	private ArrayList trackNameList;
	private ArrayList trackDescriptionList;
	private LayoutInflater mlin; //to get the Context's layout
	private Bitmap trackIcon;
	private Cursor curSor;
	
	
	static class Holder{
		TextView text;
		TextView des;
		ImageView icon;
		}
	
	public IconAdapter(Context ctx,Cursor c){
		_ctx=ctx;
		curSor=c;
		mlin=LayoutInflater.from(ctx);
		trackNameList=new ArrayList<String>();
		trackDescriptionList = new ArrayList<String>();
		
		
		//c.moveToFirst();
		for(int i=0;i<c.getCount();i++)
		{
			trackNameList.add(c.getString(1)); //track name
			trackDescriptionList.add(c.getString(2));//track description
			c.moveToNext();
		}
		
		trackIcon=BitmapFactory.decodeResource(
				_ctx.getResources(),android.R.drawable.star_big_on );
		
		
	}

	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return trackNameList.size();
	}

	@Override
	public Object getItem(int position) {
		// TODO Auto-generated method stub
		return getView(position, null, null);
	}

	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		if(curSor!=null)
		{
		//���n
			curSor.moveToPosition(position);
		return curSor.getLong(0);
		}
		else{
		return 0;
		}

	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// TODO Auto-generated method stub
		Holder holder=null;
		if(convertView==null)
		{
	    convertView=mlin.inflate(R.layout.icon, null);
		holder=new Holder();
		holder.icon=(ImageView)convertView.findViewById(R.id.icon);
		holder.text=(TextView)convertView.findViewById(R.id.icontext);
		holder.des = (TextView)convertView.findViewById(R.id.destext);
		convertView.setTag(holder);

		}
		else{
			holder=(Holder)convertView.getTag();
			}

		holder.text.setText((String)trackNameList.get(position));
		//Log.i("Message", "IconAdapter.holder.text")
		holder.des.setText((String)trackDescriptionList.get(position));
		//holder.des.setTextColor(Color.CYAN);
		holder.icon.setImageBitmap(trackIcon);
		//holder.icon.setBackgroundColor(Color.GREEN);
		return convertView;

	}
	
	

}
