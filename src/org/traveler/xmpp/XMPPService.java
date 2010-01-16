package org.traveler.xmpp;

import java.util.Collection;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.FromContainsFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.Message;

/**
 * @author tytung
 * @version 1.0
 * @date 2010/01/09
 */
public class XMPPService {
	
	public static String host = "talk.google.com";
	public static int port = 5222;
	public static String serviceName = "gmail.com";
	
	private XMPPConnection connection;
	private String xmppReceiver;
	
	public XMPPService() {
		// Create the configuration for this new connection
		ConnectionConfiguration config = new ConnectionConfiguration(host, port, serviceName);
		if (connection == null)
			connection = new XMPPConnection(config);
	}
	
	public void connect() {
		if (!connection.isConnected()) {
			try {
				connection.connect();
			} catch (XMPPException e) {
				e.printStackTrace();
			}
			System.out.println("Host: " + connection.getHost());
			System.out.println("Port: " + connection.getPort());
			System.out.println("Service: " + connection.getServiceName());
		}
	}

	public void login(String email, String password) {
		if (!connection.isAuthenticated()) {
			try {
				//connection.login("user@gmail.com", "password");
				connection.login(email, password, "bot");
			} catch (XMPPException e) {
				e.printStackTrace();
			}
		}
		
		/**
		 * The roster lets you keep track of the availability ("presence") of other users.
		 * A roster also allows you to organize users into groups such as "Friends" and "Co-workers".
		 * Other IM systems refer to the roster as the buddy list, contact list, etc.
		 */
		if (connection.isAuthenticated()) {
			System.out.println("connection.isAuthenticated() after invoking login()");
			Roster roster = connection.getRoster();
			roster.setSubscriptionMode(Roster.SubscriptionMode.accept_all);
			Collection<RosterEntry> entries = roster.getEntries();
			for (RosterEntry entry : entries) {
				System.out.println("Roster: "+entry.getUser()+" ("+entry.getType()+")");
			}
		}
	}

	public void addConnectionListener(ConnectionListener connectionListener) {
		if (connection.isConnected()) {
			connection.addConnectionListener(connectionListener);
		}
	}

	public void addPacketListener(PacketListener packetListener, String xmppReceiverFilter) {
		if (connection.isConnected()) {
			this.xmppReceiver = xmppReceiverFilter;
			// Create a packet filter to listen for new messages from a particular
			// user. We use an AndFilter to combine two other filters.
			PacketFilter packetFilter = new AndFilter(
					new PacketTypeFilter(Message.class), 
					new FromContainsFilter(xmppReceiver));
			
			connection.addPacketListener(packetListener, packetFilter);
		}
	}

	public void sendMessage(String body) throws XMPPException {
		Message msg = new Message(xmppReceiver, Message.Type.chat);
		msg.setBody(body);
		connection.sendPacket(msg);
		System.out.println(msg.toXML());
	}
	
	public void disconnect() {
		if (connection.isConnected()) {
			System.out.println("disconnect: " + connection.isConnected());
			connection.disconnect();
		}
	}

	public XMPPConnection getConnection() {
		return connection;
	}
	
	public boolean isConnected() {
		return connection.isAuthenticated();
	}
	
}
