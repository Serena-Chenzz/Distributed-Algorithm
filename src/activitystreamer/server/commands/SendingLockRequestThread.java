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

public class SendingLockRequestThread extends Thread {
    private static boolean closeConnection=false;
    private final static Logger log = LogManager.getLogger();
    
    public void run(){
       //Monitor the lockAckQueue every 1 seconds
        while(!Control.getInstance().getTerm()){
            try{
                HashMap<Connection, HashMap<Long, String>> lockAckQueue = Control.getLockAckQueue();
                if (!lockAckQueue.isEmpty()){
                    for (Connection con: lockAckQueue.keySet()){
                        HashMap<Long, String> targetMap = lockAckQueue.get(con);
                        if (!targetMap.isEmpty()){
                            for(long sendingTime:targetMap.keySet()){
                                long currentTime = System.currentTimeMillis();
                                String message = targetMap.get(sendingTime);
                                if ((!message.equals("Received Ack"))&&(currentTime - sendingTime >= 500)){
                                    String username = message.split(" ")[0];
                                    String secret = message.split(" ")[1];
                                    
                                    String senderipAddress = Control.getInstance().getUniqueId().split(" ")[0];
                                    String senderportNum = Control.getInstance().getUniqueId().split(" ")[1];
                                    JSONObject lockRequest = Command.createLockRequest(username, secret,senderipAddress,senderportNum);
                                    
                                    //Send this lock_request to relay_server
                                    String targetIpAddress = con.getRemoteId().split(" ")[0];
                                    String targetPortNum = con.getRemoteId().split(" ")[1];
                                   
                                    String relay_msg = Command.createRelayMessage(lockRequest.toJSONString(), targetIpAddress, targetPortNum);
                                    Control.getInstance().sendMessageToRandomNeighbor(relay_msg);
                                    }
                             }
                        }
                }
                Thread.sleep(500);
               }
            }catch(ConcurrentModificationException e){
                log.info("Block iterating arrays when modifying it");
            }catch (InterruptedException e){
                log.info("This thread is interrupted forcefully.");
            }
        }
        log.info("closing sending lockRequest thread....");
        closeConnection=true;
  }
    
    public static boolean getResponse() {
        return closeConnection;
    }
}
