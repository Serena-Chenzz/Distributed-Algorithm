package activitystreamer.server.commands;

import activitystreamer.server.Connection;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import activitystreamer.models.*;
import activitystreamer.server.Control;

public class Login {
    
	 
    private Connection conn;
	private String msg;
    private static final Logger log = LogManager.getLogger();
    private static boolean closeConnection=false;
    protected static Login login = null;
    
    public Login(Connection con, String msg) {
    	this.conn = con;
    	this.msg = msg;
    	
    	try {
            JSONParser parser = new JSONParser();
            JSONObject message = (JSONObject) parser.parse(msg);
            String username = message.get("username").toString();
            
            //If it is anonymous user, we can ignore the secret field
            String secret = "";
            if (!username.equals("anonymous")){
                secret = message.get("secret").toString();
            }
            
            //Start checking users
            if(username.equals("anonymous"))//Anonymous logins
            {
                JSONObject loginSuccess = Command.createLoginSuccess(username);
                conn.writeMsg(loginSuccess.toJSONString());
                closeConnection = false;
                addUser(conn,username +" "+ System.currentTimeMillis());
				Control.setConnectionClients(conn);

            }else if(Control.getInstance().checkLocalUserAndSecret(username,secret)) {  
                //If this username and secret are correct,we send back a login_success
                JSONObject loginSuccess = Command.createLoginSuccess(username);
                conn.writeMsg(loginSuccess.toJSONString());
                closeConnection = false;
                addUser(conn,username+" "+ System.currentTimeMillis());
                Control.setConnectionClients(conn);

            }else { 
                //If this username and secret are not correct, we send a login_failed
                JSONObject loginFailed = Command.createLoginFailed(username);
                conn.writeMsg(loginFailed.toJSONString());
                closeConnection = true;
            }
                
        }
        catch (ParseException e) {
            log.debug(e);
        }
    }


    //Check whether this user is logged in
    public static boolean checkUserLoggedIn(String username) {
    	for (Map.Entry<Connection,String> user : Control.getInstance().getUserConnections().entrySet()) {
    		Connection key = user.getKey();
    		String[] value = user.getValue().split(" ");
    		String targetName = value[0];
    		if (targetName.equals(username)) {
    			return true;
    		}
		}
        return false;
    }

    public static void addUser(Connection con, String username) {
        //We add a login time for each user
        long timestamp = System.currentTimeMillis();
        Control.getInstance().getUserConnections().put(con, username + " "+ timestamp);
        //Also add this connection to clientMsgBuffQueue
        Control.getInstance().initiateClientMsgBufferQueue(con);
        
	}
    
    //Logout user, remove from list
    public static void logoutUser(Connection con) {
    	for (Map.Entry<Connection,String> user : Control.getInstance().getUserConnections().entrySet()) {
    		Connection key = user.getKey();
    		if (key==con) {
    			//log.debug("deleted "+key+value);
    		    Control.getInstance().getUserConnections().remove(key);
    			break;
    		}
		}
    	//Also remove from clientMsgBuff
    	Control.getInstance().removeClientFromMsgBuffer(con);
    }

	public boolean getResponse() {
        return closeConnection;
    }

	public Connection getConnection() {
		return conn;
	}
}
