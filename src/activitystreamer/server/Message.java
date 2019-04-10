package activitystreamer.server;
import org.json.simple.JSONObject;

import activitystreamer.util.Settings;

public class Message {
    private long timestamp;
    private Connection clientConnection;
    private JSONObject message;
    
    //Create a message to be stored inside the server side
    public Message(Connection clientConnection, JSONObject activity){
        this.timestamp = System.currentTimeMillis();
        this.clientConnection = clientConnection;
        this.message = activity;       
    }
    
    //Or we can create from timestamp, clientConnection and activity
    public Message(Connection clientConnection, long timestamp, JSONObject activity ){
        this.timestamp = timestamp;
        this.clientConnection = clientConnection;
        this.message =activity;
    }
    
    public long getTimeStamp(){
        return this.timestamp;
    }
    
    public String getSenderIp(){
        return Control.getInstance().getUniqueId().split(" ")[0];
    }
    
    public int getPortNum(){
        return Integer.parseInt(Control.getInstance().getUniqueId().split(" ")[1]);
    }
    
    public JSONObject getActivity(){
        return this.message;
    }
    //Overwrite equals function
    public boolean equals(Message msg2){
        return (this.timestamp == msg2.timestamp) && 
                (this.getSenderIp().equals(msg2.getSenderIp())) &&
                (this.getPortNum()== msg2.getPortNum());
    }
    
    //This message will be transferred between servers
    @SuppressWarnings("unchecked")
    public JSONObject getTransferredMsg(){
        message.put("timestamp", this.timestamp);
        message.put("sender_ip_address", this.getSenderIp());
        message.put("sender_port_num", this.getPortNum());
        message.put("activity_content", this.message);
        return message;
    }
    
    public String toString(){
        return (this.timestamp + " ") + this.clientConnection +" "+ this.message.toJSONString();
    }

}
