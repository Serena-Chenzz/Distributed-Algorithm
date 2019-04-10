package activitystreamer.server.commands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.ConcurrentModificationException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;

import activitystreamer.server.Connection;
import activitystreamer.server.Control;
import activitystreamer.server.Message;
import activitystreamer.util.Settings;
import activitystreamer.models.*;
import activitystreamer.server.Load;

public class ActivityClientBroadcastThread extends Thread{
    
    private static boolean closeConnection=false;
    private final static Logger log = LogManager.getLogger();
    
    public ActivityClientBroadcastThread() {
        //start();
    }
    
    
    @Override
    public void run() {
        log.info("ActivityClientBroadcastThread is running");
        while(!Control.getInstance().getTerm()){
            //Fetch clientMsgBufferQueue
            try{
                HashMap<Connection, ArrayList<Message>> clientMsgBuffQueue = Control.getClientMsgBuffQueue();
                if (!clientMsgBuffQueue.isEmpty()){
                    for(Iterator<Entry<Connection, ArrayList<Message>>> it = clientMsgBuffQueue.entrySet().iterator();it.hasNext();){
                        Entry<Connection, ArrayList<Message>> newEntry = it.next();
                        Connection con = newEntry.getKey();
                        //Broadcast the first message
                        //For clients,we don't wait for acknowledgments
                        ArrayList<Message> targetList = clientMsgBuffQueue.get(con);
                        if((!(targetList == null))&&(!targetList.isEmpty())){
                            System.out.println("start sending...");
                            //Also get the client list to check the timestamp
                            String userInfo = Control.getInstance().getUserConnections().get(con);
                            long userLoginTime = Long.parseLong(userInfo.split(" ")[1]);
                            //Broadcast the first message
                            Message msg = targetList.get(0);
                            long msgSendingTime = msg.getTimeStamp();
                            //If the message is sent after the user logs in, then send the message
                            if (msgSendingTime >= userLoginTime){
                                //Then remove this message from message queue
                                Control.getInstance().removeFromClientMsgBufferQueue(con, msg);
                                log.info("Sending message..." + msg);
                                String broadMsg = Command.createActivityBroadcast(msg);
                                con.writeMsg(broadMsg);
                                
                             }
                        }
                    }
                }
                Thread.sleep(500);
            }catch(ConcurrentModificationException e){
                log.info("Block iterating arrays when modifying it");
            }catch (InterruptedException e){
                log.info("This thread is interrupted forcefully.");
            }
        }
        
        log.info("closing broadcasing clients thread....");
        closeConnection=true;
    }
    
    public static boolean getResponse() {
        return closeConnection;
    }
  


}


