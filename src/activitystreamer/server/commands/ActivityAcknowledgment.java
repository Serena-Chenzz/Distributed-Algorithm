package activitystreamer.server.commands;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.ArrayList;

import activitystreamer.models.Command;
import activitystreamer.server.Connection;
import activitystreamer.server.Control;
import activitystreamer.server.Message;

public class ActivityAcknowledgment {
    private boolean closeConnection=false;
    private final Logger log = LogManager.getLogger();

    public ActivityAcknowledgment(Connection con, String msg) {
        JSONParser parser = new JSONParser();
        JSONObject message;
        try{
            message = (JSONObject) parser.parse(msg);
            long msgTimestamp = (long)message.get("timestamp");
            String msgSenderIp = (String)message.get("sender_ip_address");
            int msgPortNum = Integer.parseInt((String) message.get("sender_port_num"));
            if (msgSenderIp.startsWith("/")){
                msgSenderIp=msgSenderIp.substring(1);
            }
            Control.getInstance().removeMessageFromBufferQueue(msgTimestamp, msgSenderIp, msgPortNum);
            //Tell activityBroadcast thread to start broadcasting next message
            Control.getInstance().activateMessageQueue(msgSenderIp + " " + msgPortNum);
            
        }
        catch (ParseException e) {
            Command.createInvalidMessage("JSON parse error while parsing message");
            closeConnection=true;
        }
    }
    public boolean getResponse() {
        return closeConnection;
    }
}
