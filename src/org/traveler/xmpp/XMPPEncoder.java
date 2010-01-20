package org.traveler.xmpp;

/**
 * @author tytung
 * @version 1.0
 * @date 2010/01/20
 */
public class XMPPEncoder {

	public static String encodeDigit(String text) {
		return convertDigit(text, 0);
	}
	
	public static String decodeDigit(String text) {
		return convertDigit(text, 1);
	}
	
	private static String convertDigit(String text, int type) {
		String output = "";
		for (int i=0; i<text.length(); i++) {
			char x = text.charAt(i);
			if (type == 0) {
				x = (char) (x + '1');
			} else {
				x = (char) (x - '1');
			}
			output += x;
		}
		return output;
	}

}
