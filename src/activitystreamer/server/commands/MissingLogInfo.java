package activitystreamer.server.commands;

import java.util.ArrayList;
import java.util.HashMap;
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

    // This class is used by the server which has missing logs. It will collect all the request logs from other servers
    // and choose the majority log for each index. The it will write the log into the log db. Also, it will perform
    // corresponding actions into ticket selling DB to keep consistent information across the system.

    public MissingLogInfo(String msg, Connection con){
        try{
            JSONParser parser = new JSONParser();
            JSONObject message = (JSONObject) parser.parse(msg);
            JSONObject values = (JSONObject) parser.parse(message.get("values").toString());
            long startIndexLong = (long)message.get("startIndex");
            long endIndexLong = (long)message.get("endIndex");
            int startIndex = (int)startIndexLong;
            int endIndex = (int) endIndexLong;

            for(Object index: values.keySet()){
                String indexString = index.toString();
                int indexInt = Integer.parseInt(indexString);
                Control.recordMissingLog(indexInt, values.get(index).toString());
            }

            int length = values.size();
            int N = Control.getInstance().getNeighbors().size();

            if(Control.getMissingAckCounter() >= length * N){
                writeIntoDB(startIndex, endIndex);
            }
            Control.cleanUpMissingLog();

        }catch (ParseException e) {
            log.debug(e);
        }

    }
    public boolean getCloseCon() {
        return closeConnection;
    }

    // Write the info into log DB as well as ticket selling DB
    public void writeIntoDB(int startIndex, int endIndex){
        HashMap<Integer, HashMap<String, Integer>> findMissingLog = Control.getFindMissingLog();
        int N = Control.getInstance().getNeighbors().size();
        for(int index = startIndex; index<=endIndex; index++){
            HashMap<String, Integer> sCounter = findMissingLog.get(index);
            String majorValue = "";
            for(String string: sCounter.keySet()){
                if(sCounter.get(string) > N/2){
                    majorValue = string;
                    break;
                }
            }

            //Write into Log DB
            Control.writeIntoLogDB(index, majorValue);

            //Also, perform the correct action to the ticket selling db locally
            Control.slavePerformAction(majorValue, index);
        }
    }

}
