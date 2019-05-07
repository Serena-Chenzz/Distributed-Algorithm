package activitystreamer.server.commands;

import java.util.ArrayList;
import java.util.Iterator;
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

import java.sql.*;

public class MissingLogInfo {

    private static Connection conn;
    private static java.sql.Connection sqlConnection;
    private static final Logger log = LogManager.getLogger();
    private static boolean closeConnection=false;
    private static final String sqlUrl =Settings.getSqlLogUrl();

    public MissingLogInfo(String msg, Connection con){
        try{
            JSONParser parser = new JSONParser();
            JSONObject message = (JSONObject) parser.parse(msg);
            JSONObject values = (JSONObject) message.get("values");

            for(Object index: values.keySet()){
                Control.recordMissingLog((int)index, values.get(index).toString());
            }

            int length = values.size();
            int N = Control.getInstance().getNeighbors().size();

            if(Control.getMissingAckCounter() >= length * N){
                // Call the GetMissingLogThread to write the contents into the db.
                GetMissingLogThread.writeIntoDB();
            }

        }catch (ParseException e) {
            log.debug(e);
        }

    }

}
