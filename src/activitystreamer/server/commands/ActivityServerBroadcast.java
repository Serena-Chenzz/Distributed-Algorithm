                                                                             package activitystreamer.server.commands;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import activitystreamer.models.Command;
import activitystreamer.server.Connection;
import activitystreamer.server.Control;
import activitystreamer.server.Message;

public class ActivityServerBroadcast {

	private boolean closeConnection=false;
	private final Logger log = LogManager.getLogger();

	public ActivityServerBroadcast(Connection con, String msg) {
		JSONParser parser = new JSONParser();
        JSONObject message;
        try {
            System.out.println("Here");
        	//check that JSON format is valid
            message = (JSONObject) parser.parse(msg);
            //Check that message is received from an Authenticated server
            if (!Control.getInstance().containsServer(con.getRemoteId())) {
                String info = "Activity_Server_Broadcast is not from an authenticated server";
                con.writeMsg(Command.createInvalidMessage(info));
                closeConnection = true;
            } else {
                //Send back an acknowledgment to the server which sent the activity message
                long timestamp = (long)message.get("timestamp");
                String senderIp = (String)message.get("sender_ip_address");
                System.out.println((message.get("sender_port_num").getClass()));
                int portNum = ((Number)message.get("sender_port_num")).intValue();
                JSONObject activity =(JSONObject)message.get("activity");
                //If it has been the latest message in the server side, the server will discard it
                if (Control.getInstance().checkAckQueue(timestamp, senderIp, portNum)){
                    String ackMsg = Command.createActivityAcknowledgemnt(timestamp, Control.getInstance().getUniqueId());
                    System.out.println("Sending Acknowledgment...");
                    String relay_msg = Command.createRelayMessage(ackMsg, senderIp, portNum + "");
                    Control.getInstance().sendMessageToRandomNeighbor(relay_msg);
                    Control.getInstance().updateAckQueue(timestamp, senderIp, portNum);
                    
                    Message newMsg = new Message(con,timestamp,activity);
                    Control.getInstance().addToAllClientMsgBufferQueue(newMsg);
            		closeConnection=false;
            		}
                //else,it will send the acknowledgement again
                else{
                    String ackMsg = Command.createActivityAcknowledgemnt(timestamp, Control.getInstance().getUniqueId());
                    System.out.println("Sending Acknowledgment Again...");
                    String relay_msg = Command.createRelayMessage(ackMsg, senderIp, portNum + "");
                    Control.getInstance().sendMessageToRandomNeighbor(relay_msg);
                    closeConnection=false;
                }
            }
        } catch (ParseException e) {
        	Command.createInvalidMessage("JSON parse error while parsing message");
        	closeConnection=true;
        } catch (ClassCastException e2){
            Command.createInvalidMessage("Invalid Content Type");
            closeConnection=true;
        }
	}
		
	public boolean getResponse() {
		return closeConnection;
	}

}