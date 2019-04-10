package activitystreamer.server.commands;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;



import activitystreamer.models.*;
import activitystreamer.server.Connection;
import activitystreamer.server.Control;
import activitystreamer.util.Settings;

public class Register {
    
    private static Connection conn;
    private static final Logger log = LogManager.getLogger();
    private static boolean closeConnection=false;
    
    
    public Register(String msg, Connection con) {
        Register.conn = con;
        
         try {
            JSONParser parser = new JSONParser();
            JSONObject message = (JSONObject) parser.parse(msg);
            //Read in the username and secret
            String username = message.get("username").toString();
            String secret =  message.get("secret").toString();
            
            //Check whether this user is in the local storage
            if(Control.getInstance().checkLocalUser(username)) { 
                //If this user is in the local storage, we directly send a register_failed
                log.info("This user has already been in the local list");
                JSONObject registerFailed = Command.createRegisterFailed(username);
                conn.writeMsg(registerFailed.toJSONString());
                log.debug(registerFailed.toJSONString());
                //The the connetion will be closed
                closeConnection = true;
            }
            //If this server has no connected servers, it will skip the broadcast part. And return the register success method
            else if (Control.getInstance().getConnectionServers().size()==0){
                //Add this user to registerList
                log.info("Adding the user to local storage");
                Control.getInstance().addLocalUser(username, secret);
                JSONObject registerSuccess = Command.createRegisterSuccess(username);
                conn.writeMsg(registerSuccess.toJSONString());
                log.debug(registerSuccess.toJSONString());
                //Adding user to list of users logged in
                closeConnection = false;
            }
            else {
            	log.info("Start broadcasting lock_request");
            	String ipAddress = Control.getInstance().getUniqueId().split(" ")[0];
            	String portNum = Control.getInstance().getUniqueId().split(" ")[1];
                Control.getInstance().addUserToRegistePendingList(username, secret, con);
                JSONObject lockRequest = Command.createLockRequest(username, secret, ipAddress,portNum);
                Control.getInstance().broadcast(lockRequest.toJSONString());
                
                for (Connection con2: Control.getNeighbors()){
                    Control.getInstance().setLockAckQueue(con2, username + " " +secret);
                }
                log.debug(lockRequest.toJSONString());
                closeConnection = false;
            }
                
        }
        catch (ParseException e) {
            log.debug(e);
        }
    }

    public boolean getCloseCon() {
        return closeConnection;
    }
}
