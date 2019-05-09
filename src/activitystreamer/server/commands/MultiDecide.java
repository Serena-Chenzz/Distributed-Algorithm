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

public class MultiDecide {
    private static Connection conn;
    private static final Logger log = LogManager.getLogger();
    private static boolean closeConnection=false;

    public MultiDecide(String msg, Connection con){
        conn = con;
        try{
            JSONParser parser = new JSONParser();
            JSONObject message = (JSONObject) parser.parse(msg);
            long indexLong = (long)message.get("index");
            int index = (int)indexLong;
            String value = message.get("value").toString();

            //First, check if its firstUnchosenIndex < leader's firstUnchosenInde, if so, write the missing logs into the db
            int myFirstUnchosenIndex = Control.getFirstUnchosenIndex();
            if(myFirstUnchosenIndex < index){
                // Notify the user about the error
                log.error("Please restart the system, the system is in an inconsistent state...");
                System.exit(1);
            }
            else{
                Control.writeIntoLogDB(index, value);
                Control.slavePerformAction(value, index);
                //Remove from UnChosenLogs
                Control.removeFromUnchosenLogs();
                closeConnection = false;
            }


        }catch (ParseException e) {
            log.debug(e);
        }
    }
    public boolean getCloseCon() {
        return closeConnection;
    }
}
