package org.traveler.track_manage.view;

import java.sql.Time;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
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
	private ArrayList<String> trackNameList;
	private ArrayList<String> trackDescriptionList;
	private ArrayList<Long> trackConsumedTimeList;
	private ArrayList<Float> trackDistanceList;
	private ArrayList<Double> trackAverageSpeedList;
	private ArrayList<Double> trackManximumSpeedList;
	private ArrayList<Long> trackPointNumberList;
	private ArrayList<String> trackSourceList;
	private String trackSource;
	
	private LayoutInflater mlin; //to get the Context's layout
	private Bitmap trackIcon;
	private Cursor curSor;
	
	
	static class Holder{
		TextView text;
		TextView des;
		TextView source;
		TextView measure;
		ImageView icon;
		}
	
	public IconAdapter(Context ctx,Cursor c){
		_ctx=ctx;
		curSor=c;
		mlin=LayoutInflater.from(ctx);
		trackNameList=new ArrayList<String>();
		trackDescriptionList = new ArrayList<String>();
		trackConsumedTimeList = new ArrayList<Long>();
		trackDistanceList = new ArrayList<Float>();
		trackAverageSpeedList = new ArrayList<Double>();
		trackManximumSpeedList = new ArrayList<Double>();
		trackPointNumberList = new ArrayList<Long>();
		trackSourceList = new ArrayList<String>();
		
		//c.moveToFirst();
		for(int i=0;i<c.getCount();i++)
		{
			trackNameList.add(c.getString(1)); //track name
			trackDescriptionList.add(c.getString(2));//track description
			//-----------Track Measurement------------------------------------------------------
			trackConsumedTimeList.add(c.getLong(6));// track consumedTime
			trackDistanceList.add(c.getFloat(7));// track totalDistance
			trackAverageSpeedList.add(c.getDouble(8));//track AverageSpeed
			trackManximumSpeedList.add(c.getDouble(9));//track ManximumSpeed
			trackPointNumberList.add(c.getLong(10));// track Point Number
			//---------------------------------------------------------------------------------
			trackSourceList.add(c.getString(11));// track source
			
			c.moveToNext();
		}
		
		
		
		
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
		holder.icon=(ImageView)convertView.findViewById(R.id.track_icon);
		holder.text=(TextView)convertView.findViewById(R.id.track_name);
		holder.des = (TextView)convertView.findViewById(R.id.destext);
		holder.source = (TextView)convertView.findViewById(R.id.track_source);
		holder.measure = (TextView)convertView.findViewById(R.id.track_meaure);
		convertView.setTag(holder);

		}
		else{
			holder=(Holder)convertView.getTag();
			}

		holder.text.setText((String)trackNameList.get(position));
		//Log.i("Message", "IconAdapter.holder.text")
		//holder.des.setText((String)trackDescriptionList.get(position));
		holder.des.setText("");
		//holder.des.setTextColor(Color.CYAN);
		
		//holder.icon.setBackgroundColor(Color.GREEN);
		String track_source = generateTrackSourceString((String)trackSourceList.get(position));
		holder.source.setText(track_source);
		if(((String)trackSourceList.get(position)).equalsIgnoreCase(trackSource))
			trackIcon=BitmapFactory.decodeResource(_ctx.getResources(),R.drawable.track_icon_flag_red );
		else
			trackIcon=BitmapFactory.decodeResource(_ctx.getResources(),R.drawable.track_icon_flag_blue );
		holder.icon.setImageBitmap(trackIcon);
		
		long time = (long)trackConsumedTimeList.get(position);
		String track_consumedTime = generateTimeString(time,this._ctx);
		float distance = trackDistanceList.get(position);
		String track_distance = generateDistanceString(distance,this._ctx);
		long trackPointNumber = trackPointNumberList.get(position);
		String track_point_number = generateTrackNumberString(trackPointNumber);
		//double averageSpeed = trackAverageSpeedList.get(position);
		//double maximumSpeed = trackManximumSpeedList.get(position);
		holder.measure.setText(track_consumedTime+"  "+track_distance+"  "+track_point_number);
		
		return convertView;

	}
	
	public void setTrackSource(String trackSource)
	{
		
		this.trackSource = trackSource;
	}
	
	
	public static String generateTimeString(long time, Context context)
	{
		
		String timeString = "";
		Time myTime = new Time(time);
		SimpleDateFormat formatter = new SimpleDateFormat("H:m:s");
		String time_string = formatter.format(myTime);
		//String time_string = myTime.toString();
		String[] time_array = time_string.split(":");
		Log.i("Message", "time_string="+time_string+",hr="+time_array[0]+",min="+time_array[1]+",sec="+time_array[2]);
		String hr = context.getString(R.string.hr_unit);
		String min = context.getString(R.string.min_unit);
		String sec = context.getString(R.string.sec_unit);
		int time_hr = Integer.parseInt(time_array[0])-8;
		String time_hr_str = Integer.toString(time_hr);
		if(time_hr_str.equals("0"))
		{
			if(time_array[1].equals("0"))
				timeString = time_array[2]+sec;
			else
				timeString = time_array[1]+min+time_array[2]+sec;
			
		}	
		else
			timeString = time_array[0]+hr+time_array[1]+min+time_array[2]+sec;
		
		Log.i("Message", "time String="+timeString);
		return timeString;
		
		
		
	}
	
	public static String generateDistanceString(float distance,Context context)
	{
	  String distanceString ="";
	  NumberFormat formatter = new DecimalFormat("#");
	    
	  String distance_unit = context.getString(R.string.meter_unit);
	  distanceString = formatter.format(distance)+distance_unit;
	  return distanceString;
		
		
	}
	
	public static String generateSpeedString(double speed,Context context)
	{
	  String speedString ="";
	  NumberFormat formatter = new DecimalFormat("#");
	    
	  String distance_unit = context.getString(R.string.speed_unit);
	  speedString = formatter.format(speed)+distance_unit;
	  return speedString;
		
		
	}
	
	public static String generateDescriptionString(String description,Context context)
	{
	  Log.i("Message", "IconAdpter.generateDescriptionString is called...");
	 
	  if(description.equalsIgnoreCase("no track description"))// "no track description" is the default value if the track file has no description about it self, see 
		                                                     // ParsedExampleDataSet.java
	  {
		  return context.getString(R.string.no_track_description);
	  } 
	  else
	  return description;
		
		
	}
	
	private String generateTrackNumberString(long number)
	{
		
		String numberString = "";
		String point_number_unit = _ctx.getString(R.string.point_number_unit);
		numberString = number+point_number_unit;
		return numberString;
		
	}
	
	private String generateTrackSourceString(String source)
	{
		
		String sourceString = "";
		String sourceText = _ctx.getString(R.string.source_text);
		return sourceText+":"+source;
		
		
	}
	
	

}
