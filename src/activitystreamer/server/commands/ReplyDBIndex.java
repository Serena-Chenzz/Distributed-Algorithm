package activitystreamer.server.commands;

import activitystreamer.server.Connection;

import java.util.HashMap;
import java.util.Map;

import activitystreamer.util.Settings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import activitystreamer.models.*;
import activitystreamer.server.Control;


public class ReplyDBIndex {
    private Connection conn;
    private String msg;
    private static final Logger log = LogManager.getLogger();
    private static boolean closeConnection=false;

    public ReplyDBIndex(String msg, Connection con){
        conn = con;
        try{
            JSONParser parser = new JSONParser();
            JSONObject message = (JSONObject) parser.parse(msg);
            long addIndexLong = (long)message.get("index");
            int addIndex = (int)addIndexLong;
            Control.appendDBIndexList(addIndex);

            if(Control.checkDBIndexListSize()){
                if(Control.checkDBIndexList()){
                    if (Control.getInstance().getNeighbors().size() > 0) {
                        log.info("Now making new selection.");
                        Control.getInstance().sendSelection(Control.getInstance().getLamportTimeStamp());
                    }
                }
                Control.cleanDBIndexList();
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
