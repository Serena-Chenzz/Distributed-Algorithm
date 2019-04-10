package activitystreamer.server.commands;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import activitystreamer.models.Command;
import activitystreamer.models.User;
import activitystreamer.server.Connection;
import activitystreamer.server.Control;
import activitystreamer.models.*;

public class RegisterSuccessBroadcast {

    private final Logger log = LogManager.getLogger();
    private boolean closeConnection=false;

    
    public RegisterSuccessBroadcast(String msg, Connection con) {
        try{
            //First, I need to check whether this connection is authenticated.
            //If not authenticated, it will return a invalid message
            if (!Control.getInstance().containsServer(con.getRemoteId())) {
                String info = "RegisterSuccessBroadcast is not from an authenticated server";
                con.writeMsg(Command.createInvalidMessage(info));
                closeConnection = true;
            }
            else{
                JSONParser parser = new JSONParser();
                JSONObject message = (JSONObject)parser.parse(msg);
                String username= message.get("username").toString();
                String secret= message.get("secret").toString();
                
                //Add this user to the local userlist
                if (!Control.getInstance().checkLocalUser(username)){
                    Control.getInstance().addLocalUser(username, secret);
                    closeConnection = false;
                }
                }
        }catch(ParseException e){
            e.printStackTrace();
        }
    }
    
    public boolean getCloseCon() {
        return closeConnection;
    }
}


