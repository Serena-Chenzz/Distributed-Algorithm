package activitystreamer.server.commands;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;

import activitystreamer.server.Connection;
import activitystreamer.server.Control;
import activitystreamer.util.Settings;

import activitystreamer.models.*;
import activitystreamer.server.Load;
import java.net.InetAddress;
import java.net.UnknownHostException;


public class ServerAnnounce extends Thread{
    
    private static boolean closeConnection=false;
    private final static Logger log = LogManager.getLogger();
    InetAddress ip;
    
    public ServerAnnounce() {
    	//start();
    	try {
            ip = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
        	log.error(e);
        }
    }
    
    
    @Override
    public void run() {
        
    	int load = 0;
    	HashMap<Connection,Boolean> connections = Control.getInstance().getConnections();
    	while(!Control.getInstance().getTerm()){
				// do something with 5 second intervals in between
				try {
					load = Load.getOwnLoad();
					JSONObject serverAnnounce;
					if(Settings.getLocalHostname().equals("localhost")) {
						serverAnnounce = Command.createServerAnnounce(
								Control.getInstance().getUniqueId(),load,ip.getHostAddress(),Settings.getLocalPort()
						);
					}else {
						serverAnnounce = Command.createServerAnnounce(
								Control.getInstance().getUniqueId(),load,Settings.getLocalHostname(),Settings.getLocalPort()
						);
					}
					 
					Control.getInstance().broadcast(serverAnnounce.toJSONString());
					Control.getInstance().printRegisteredUsers();
					Thread.sleep(Settings.getActivityInterval());
				} catch (InterruptedException e) {
					log.info("received an interrupt, system is shutting down");
					break;
				}
			}
	   	if(!Control.getInstance().getTerm()){
			log.debug("doing activity");
			Control.getInstance().setTerm(doActivity());
		}
		
		log.info("closing "+connections.size()+" connections");
		// clean up
        //Use iterator to avoid concurrency issues
        for(Iterator<Entry<Connection, Boolean>> it = connections.entrySet().iterator();it.hasNext();){
            Entry<Connection, Boolean> newEntry = it.next();
            Connection con = newEntry.getKey();
            con.closeCon();
        }
            
		Control.getInstance().listenAgain();;
    }
    
    public static boolean doActivity() {
        return false;
    }
    public static boolean getResponse() {
        return closeConnection;
    }


}
