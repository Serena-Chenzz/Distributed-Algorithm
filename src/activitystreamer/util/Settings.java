package activitystreamer.util;

import java.math.BigInteger;
import java.net.Socket;
import java.security.SecureRandom;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class Settings {
	private static final Logger log = LogManager.getLogger();
	private static SecureRandom random = new SecureRandom();
	private static int localPort = 3780;
	private static String localHostname = "localhost";
	private static String remoteHostname = null;
	private static int remotePort = 3780;
	private static int activityInterval = 5000; // milliseconds
	private static String secret = null;
	private static String username = "anonymous";
	private static String sqlUrl = "jdbc:sqlite:/Users/luchen/Documents/Documents/Melb_Uni_Life/Semester4/Distributed Algorithm/Project/Distributed-Algorithm/dbs/UserTest.db";
	private static String sqlLogUrl = "jdbc:sqlite:/Users/luchen/Documents/Documents/Melb_Uni_Life/Semester4/Distributed Algorithm/Project/Distributed-Algorithm/dbs/log.db";
    private static boolean initiateElection = false;

    public static void setInitiateElection(String flag){if(flag.equals("true")){initiateElection = true;}}
	public static boolean getInitiateElection(){return initiateElection;}
	public static int getLocalPort() {
		return localPort;
	}

	public static void setLocalPort(int localPort) {
		if(localPort<0 || localPort>65535){
			log.error("supplied port "+localPort+" is out of range, using "+getLocalPort());
		} else {
			Settings.localPort = localPort;
		}
	}
	
	public static int getRemotePort() {
		return remotePort;
	}

	public static String getSqlUrl() {return sqlUrl;}

	public static String getSqlLogUrl() {return sqlLogUrl;}

	public static void setRemotePort(int remotePort) {
		if(remotePort<0 || remotePort>65535){
			log.error("supplied port "+remotePort+" is out of range, using "+getRemotePort());
		} else {
			Settings.remotePort = remotePort;
		}
	}
	
	public static String getRemoteHostname() {
		return remoteHostname;
	}

	public static void setRemoteHostname(String remoteHostname) {
		Settings.remoteHostname = remoteHostname;
	}
	
	public static int getActivityInterval() {
		return activityInterval;
	}

	public static void setActivityInterval(int activityInterval) {
		Settings.activityInterval = activityInterval;
	}
	
	public static String getSecret() {
		if (secret == null) {
			return "";
		}else {
			return secret;
		}
	}

	public static void setSecret(String s) {
		secret = s;
	}
	
	public static String getUsername() {
		return username;
	}

	public static void setUsername(String username) {
		Settings.username = username;
	}
	
	public static String getLocalHostname() {
		return localHostname;
	}

	public static void setLocalHostname(String localHostname) {
		Settings.localHostname = localHostname;
	}

	
	/*
	 * some general helper functions
	 */
	
	public static String socketAddress(Socket socket){
		return socket.getInetAddress()+":"+socket.getPort();
	}

	public static String nextSecret() {
	    return new BigInteger(130, random).toString(32);
	 }
	
	
}
