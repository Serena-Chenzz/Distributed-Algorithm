package activitystreamer.server.commands;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import activitystreamer.models.*;
import activitystreamer.server.Connection;
import activitystreamer.server.Control;

public class RelayMessage {

    private static final Logger log = LogManager.getLogger();
    private boolean closeConnection = false;

    public RelayMessage(String msg, Connection con) {

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
                String targetIp = message.get("target_ip_address").toString();
                String targetPort =  message.get("target_port_num").toString();
                String relayMsg = message.get("relay_message").toString();
                //First, check if the target is this server
                //If it is, deal with it directly
                String localUniqueId = Control.getInstance().getUniqueId();
                System.out.println(localUniqueId);
                System.out.println(targetIp + " " + targetPort);
                if (targetIp.startsWith("/")){
                    targetIp=targetIp.substring(1);
                }
                if(localUniqueId.equals(targetIp + " " + targetPort)){
                    System.out.println(relayMsg);
                    boolean result = Control.getInstance().process(con, relayMsg);
                    closeConnection =result;
                }
                else{
                    //Send message to the target server
                    for (Connection nei:Control.getNeighbors()){
                        if (nei.getRemoteId().equals(targetIp + " " + targetPort)){
                            nei.writeMsg(msg);
                        }
                    }
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

