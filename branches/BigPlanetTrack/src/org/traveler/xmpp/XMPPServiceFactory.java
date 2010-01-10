package org.traveler.xmpp;

/**
 * A factory for creating a singleton instance of {@link XMPPService}.
 * 
 * @author tytung
 * @version 1.0
 * @date 2010/01/09
 */
public class XMPPServiceFactory {
	private static XMPPService xmppService;
	
	/** This class should not be instantiated. */
	private XMPPServiceFactory() {}
	
	/**
	 * Create a new {@link XMPPService} instance.
	 * 
	 * @return an instance of {@link XMPPService}.
	 */
	public static synchronized XMPPService getXMPPService() {
		if (xmppService == null)
			xmppService = new XMPPService();
		return xmppService;
	}
}