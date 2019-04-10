package activitystreamer.server.commands;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import activitystreamer.models.*;
import activitystreamer.server.Connection;
import activitystreamer.server.Control;

public class Lock {

    private static final Logger log = LogManager.getLogger();
    private boolean closeConnection = false;

    public Lock(String msg, Connection con) {

        try {
            //First, I need to check whether this connection is authenticated.
            //If not authenticated, it will return a invalid message
            if (!Control.getInstance().containsServer(con.getRemoteId())) {
                String info = "Lock_Request is not from an authenticated server";
                con.writeMsg(Command.createInvalidMessage(info));
                closeConnection = true;
            } else {
                JSONParser parser = new JSONParser();
                JSONObject message = (JSONObject) parser.parse(msg);
                String username = message.get("username").toString();
                String secret =  message.get("secret").toString();
                String target_ip = message.get("sender_ip_address").toString();
                String target_port = message.get("sender_port_num").toString();
                
                String sender_ip = Control.getInstance().getUniqueId().split(" ")[0];
                String sender_port = Control.getInstance().getUniqueId().split(" ")[1];
                
                
                //Check if this user exists
                if (Control.getInstance().checkLocalUser(username)||Control.getInstance().checkRegisterPendingList(username)) {
                    
                    JSONObject lockDenied = Command.createLockDenied(username, secret, sender_ip, sender_port);
                    String relayMsg = Command.createRelayMessage(lockDenied.toJSONString(), target_ip, target_port);
                    Control.getInstance().sendMessageToRandomNeighbor(relayMsg);

                } else {
                    //If the user is not in the local storage,
                    //Send back lock_allowed
                    JSONObject lockAllowed = Command.createLockAllowed(username, secret, sender_ip, sender_port);
                    String relayMsg = Command.createRelayMessage(lockAllowed.toJSONString(), target_ip, target_port);
                    Control.getInstance().sendMessageToRandomNeighbor(relayMsg);
                    
                    closeConnection = false;
             
                }
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public boolean getCloseCon() {
        return closeConnection;
    }
}
