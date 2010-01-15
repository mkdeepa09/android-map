package org.traveler.track_manage.file.operate;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.util.Log;

public class ExampleHandler extends DefaultHandler {
	
	// =========================================================== 
    // Fields 
    // =========================================================== 
     
    private boolean in_name_tag = false; 
    private boolean in_desc_tag = false; 
    private boolean in_trkseg_tag = false;
    private boolean in_trkpt_tag = false;
    private boolean in_time_tag = false;
    private boolean in_ele_tag = false;
    
    private TrackPoint trackPoint = null;
    private ParsedExampleDataSet myParsedExampleDataSet = new ParsedExampleDataSet();; 
    
    // =========================================================== 
    // Getter & Setter 
    // =========================================================== 

    public ParsedExampleDataSet getParsedData() { 
         return this.myParsedExampleDataSet; 
    } 

    // =========================================================== 
    // Methods 
    // =========================================================== 
    @Override 
    public void startDocument() throws SAXException { 
        //this.myParsedExampleDataSet = new ParsedExampleDataSet();
    	Log.i("Message:","startDocument");
    } 

    @Override 
    public void endDocument() throws SAXException { 
         // Nothing to do 
    	Log.i("Message:","endDocument");
    } 

    /** Gets be called on opening tags like: 
     * <tag> 
     * Can provide attribute(s), when xml was like: 
     * <tag attribute="attributeValue">*/ 
    @Override
	public void startElement(String namespaceURI, String localName, String qName,
			Attributes attributes) throws SAXException {
		// TODO Auto-generated method stub
		//super.startElement(namespaceURI, localName, qName, attributes);
    	Log.i("TAG-localName",localName);
    	Log.i("TAG-qName",qName);
		 if (localName.equals("name")) { 
             this.in_name_tag = true;
             Log.i("Message","in name_tag");
        }else if (localName.equals("desc")) { 
             this.in_desc_tag = true;
             Log.i("Message","in desc_tag");
        }else if (localName.equals("trkseg")) { 
             this.in_trkseg_tag = true;
             Log.i("Message","in trkseg_tag");
        }else if (localName.equals("trkpt")) { 
             // Extract an Attribute 
             //String attrValue = attributes.getValue("thenumber");
        	this.in_trkpt_tag = true;
        	Log.i("Messgae","in trkpt_tag,lat="+attributes.getValue("lat")+",lon="+attributes.getValue("lon"));
        	Log.i("Attribute",attributes.toString());
        	Log.i("lat",attributes.getValue("lat"));
        	Log.i("lon",attributes.getValue("lon"));
        	this.trackPoint = new TrackPoint();
        	this.trackPoint.setLatitude(Double.parseDouble(attributes.getValue("lat")));
        	this.trackPoint.setLongitude(Double.parseDouble(attributes.getValue("lon")));
        	
        	Log.i("Messgae","create a new trackPoint and store lat,lon");
        	
        	
        }else if (localName.equals("time")) { 
        	this.in_time_tag = true;
        	Log.i("Messgae","in time_tag");
        }
        else if (localName.equals("ele")) { 
        	this.in_ele_tag = true;
        	Log.i("Messgae","in ele_tag");
        }else{
        	Log.i("Messgae","in other tag");
        }
	}

	
   
     
    /** Gets be called on closing tags like: 
     * </tag> */ 
    @Override 
    public void endElement(String namespaceURI, String localName, String qName) 
              throws SAXException { 
         if (localName.equals("name")) { 
              this.in_name_tag = false;
              Log.i("Messgae","out name_tag");
         }else if (localName.equals("desc")) { 
              this.in_desc_tag = false;
              Log.i("Messgae","out desc_tag");
         }else if (localName.equals("trkseg")) { 
              this.in_trkseg_tag = false;
              Log.i("Messgae","out trkseg_tag");
         }else if (localName.equals("trkpt")) { 
              this.in_trkpt_tag = false;
              
              Pattern p = Pattern.compile("(20\\d{2})-(0[0-9]|1[0-2])-(0[1-9]|[12][0-9]|3[01])(T|\\s)([0-1][0-9]|2[0-3]):([0-5][0-9]):([0-5][0-9])(Z|.0)");
              Matcher m = p.matcher(this.trackPoint.getTime());
              if(m.matches()){
               Log.i("Message", "Time format checking pass....");
               this.trackPoint.computeTimeLongValue();
               this.myParsedExampleDataSet.addTrackPoint(this.trackPoint);
               this.trackPoint = null;
               Log.i("Messgae","out trkpt_tag");
               Log.i("Messgae","add a new track_point into arrayList");
               Log.i("Messgae","reset trackPoint Null");
              }
              else
              {
            	  Log.i("Message", "Time format checking Not pass...., time="+this.trackPoint.getTime());
            	  this.trackPoint = null;
            	  Log.i("Messgae","out trkpt_tag");
            	  Log.i("Messgae","reset trackPoint Null");
              }
              
         }else if (localName.equals("time")) { 
              this.in_time_tag = false;
              Log.i("Messgae","out time_tag");
         }else if (localName.equals("ele")) { 
             this.in_ele_tag = false;
             Log.i("Messgae","out ele_tag");
         }       
         
    } 
     
    /** Gets be called on the following structure: 
     * <tag>characters</tag> */ 
    @Override 
   public void characters(char[] ch, int start, int length) { 
         if(this.in_name_tag){
        	 Log.i("Message:","catch name");
        	 String name = new String(ch, start, length);
        	 Log.i("Message:","name="+name);
             myParsedExampleDataSet.setTrackName(name); 
          }else if(this.in_desc_tag){
        	 Log.i("Message:","catch desc");
         	 String desc = new String(ch, start, length);
         	 Log.i("Message:","desc="+desc);
             myParsedExampleDataSet.setTrackDescription(desc);
        	  
          }else if(this.in_time_tag){
         	 Log.i("Message:","catch time");
          	 String time = new String(ch, start, length);
          	 Log.i("Message:","time="+time);
             this.trackPoint.setTime(time);
             Log.i("Message:","store time");
         	  
           }else if(this.in_ele_tag){
         	 Log.i("Message:","catch elevation");
          	 String elevation = new String(ch, start, length);
          	 Log.i("Message:","elevation="+elevation);
             this.trackPoint.setElevation(elevation);
             Log.i("Message:","store elevation");
         	  
           }
         
   } 

}
