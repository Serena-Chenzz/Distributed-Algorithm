package activitystreamer.models;

import java.util.ArrayList;

import org.json.simple.JSONObject;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import activitystreamer.server.Message;

//ACTIVITY_SERVER_BROADCAST, ACTIVITY_ACKNOWLEDGEMENT, REGISTER_SUCCESS_BROADCAST are the commands we added
public enum Command {
    AUTHENTICATE, INVALID_MESSAGE, AUTHENTICATION_FAIL, AUTHENTICATION_SUCCESS, LOGIN, LOGIN_SUCCESS, 
    REDIRECT, LOGIN_FAILED, LOGOUT, ACTIVITY_MESSAGE, SERVER_ANNOUNCE,
    ACTIVITY_BROADCAST, REGISTER, REGISTER_FAILED, REGISTER_SUCCESS, LOCK_REQUEST, 
    LOCK_DENIED, LOCK_ALLOWED, ACTIVITY_SERVER_BROADCAST, ACTIVITY_ACKNOWLEDGEMENT, REGISTER_SUCCESS_BROADCAST,
    USERS_REGISTERED_LIST, RELAY_MESSAGE;


    public static boolean contains(String commandName) {
        for (Command c : Command.values()) {
            if (c.name().equals(commandName)) {
                return true;
            }
        }
        return false;
    }
    
    //Create relay_message
    @SuppressWarnings("unchecked")
    public static String createRelayMessage(String message, String targetIp, String targetPortNum ){
        JSONObject obj = new JSONObject();
        obj.put("command", RELAY_MESSAGE.toString());  
        obj.put("target_ip_address", targetIp);
        obj.put("target_port_num", targetPortNum);
        obj.put("relay_message", message);
        return obj.toJSONString();  
    }
    
    //Create LOGIN JSON object.
    @SuppressWarnings("unchecked")
    public static JSONObject createLogin(String username, String secret){
        JSONObject obj = new JSONObject();
        if (username.equals("anonymous")){
            obj.put("command", LOGIN.toString());
            obj.put("username", username);
        }
        else{
            obj.put("command", LOGIN.toString());
            obj.put("username", username);
            obj.put("secret", secret);
        }  
        return obj;  
    }
    @SuppressWarnings("unchecked")
    public static JSONObject createLogout(){
        JSONObject obj = new JSONObject();
        obj.put("command", LOGOUT.toString());    
        return obj;  
    }
    
    @SuppressWarnings("unchecked")
    public static JSONObject createLoginFailed(String username){
        JSONObject obj = new JSONObject();
        obj.put("command", LOGIN_FAILED.toString());
        obj.put("info", "attempt to login with wrong secret");
        
        return obj;
    } 
    
    @SuppressWarnings("unchecked")
    public static JSONObject createLoginSuccess(String username){
        JSONObject obj = new JSONObject();
        obj.put("command", LOGIN_SUCCESS.toString());
        obj.put("info", "logged in as user " + username);
        
        return obj;
    }
    @SuppressWarnings("unchecked")
    public static String createInvalidMessage(String info){
        JSONObject obj = new JSONObject();
        obj.put("command", INVALID_MESSAGE.toString());
        obj.put("info", info); 
        return obj.toJSONString();
        
    }
    
    @SuppressWarnings("unchecked")
    public static String getInvalidMessage(JSONObject obj) {
        return (String)obj.get("info");
    }
    
    //Create REGISTER JSON object.
    @SuppressWarnings("unchecked")
    public static JSONObject createRegister(String username, String secret){
        JSONObject obj = new JSONObject();
        obj.put("command", REGISTER.toString());
        obj.put("username", username);
        obj.put("secret", secret);
        
        return obj;
        
    }
    
    //Create LOCK_REQUEST JSON object.
    @SuppressWarnings("unchecked")
    public static JSONObject createLockRequest(String username, String secret, String ipAddress, String portNum){
        JSONObject obj = new JSONObject();
        obj.put("command", LOCK_REQUEST.toString());
        obj.put("username", username);
        obj.put("secret", secret);
        obj.put("sender_ip_address", ipAddress);
        obj.put("sender_port_num", portNum);
        return obj;
        
    }
    
    
    @SuppressWarnings("unchecked")
    public static JSONObject createLockDenied(String username, String secret, String ipAddress, String portNum){
        JSONObject obj = new JSONObject();
        obj.put("command", LOCK_DENIED.toString());
        obj.put("username", username);
        obj.put("secret", secret);
        obj.put("sender_ip_address", ipAddress);
        obj.put("sender_port_num", portNum);
        
        return obj;
    }
    
    @SuppressWarnings("unchecked")
    public static JSONObject createLockAllowed(String username, String secret, String ipAddress, String portNum){
        JSONObject obj = new JSONObject();
        obj.put("command", LOCK_ALLOWED.toString());
        obj.put("username", username);
        obj.put("secret", secret);
        obj.put("sender_ip_address", ipAddress);
        obj.put("sender_port_num", portNum);
        
        return obj;
    }
    
    @SuppressWarnings("unchecked")
    public static JSONObject createRegisterSuccess(String username){
        JSONObject obj = new JSONObject();
        obj.put("command", REGISTER_SUCCESS.toString());
        obj.put("info", "register success for " + username);
        
        return obj;
    } 
    
    @SuppressWarnings("unchecked")
    public static JSONObject createRegisterSuccessBroadcast(String username, String secret){
        JSONObject obj = new JSONObject();
        obj.put("command", REGISTER_SUCCESS_BROADCAST.toString());
        obj.put("username", username);
        obj.put("secret", secret);     
        return obj;
    }
    
    
    @SuppressWarnings("unchecked")
    public static JSONObject createRegisterFailed(String username){
        JSONObject obj = new JSONObject();
        obj.put("command", REGISTER_FAILED.toString());
        obj.put("info", username + " is already registered with the system");
         
        return obj;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject createAuthenticate(String secret, String uniqueId) {
        JSONObject obj = new JSONObject();
        obj.put("command", AUTHENTICATE.toString());
        obj.put("secret",secret);
        obj.put("remoteId", uniqueId);        
        return obj;
    }
    
    @SuppressWarnings("unchecked")
    public static String createAuthenticateSuccess(ArrayList<String> neighborInfo, String uniqueId) {
        JSONObject obj = new JSONObject();
        obj.put("command", AUTHENTICATION_SUCCESS.toString());
        obj.put("info",neighborInfo); 
        obj.put("remoteId", uniqueId); 
        return obj.toJSONString();
    }
    
    @SuppressWarnings("unchecked")
    public static String createRedirect(String hostname, String port){
        JSONObject obj = new JSONObject();
        obj.put("command", Command.REDIRECT.toString());
        obj.put("hostname", hostname);
        obj.put("port", port);
        return obj.toJSONString();
    }
    
    @SuppressWarnings("unchecked")
    public static JSONObject createServerAnnounce(String uniqueId, int load, String hostname, int port) {
        JSONObject obj = new JSONObject();
        obj.put("command", Command.SERVER_ANNOUNCE.toString());
        obj.put("id", uniqueId);
        obj.put("load", load);
        obj.put("hostname", hostname);
        obj.put("port", port);
           
       return obj;
    }

    
    @SuppressWarnings("unchecked")
    public static String createAuthenticateFailed(String secret, String uniqueId) {
        JSONObject obj = new JSONObject();
        obj.put("command", Command.AUTHENTICATION_FAIL.toString());
        obj.put("info", "the supplied secret is incorrect: "+secret);
        obj.put("remoteId", uniqueId);
       return obj.toJSONString();
    }
    
    @SuppressWarnings("unchecked")
	public static String createAuthFailedUserNotLoggedIn(String username) {
    	JSONObject obj = new JSONObject();
        obj.put("command", Command.AUTHENTICATION_FAIL.toString());
        obj.put("info", "the user '"+username+"' has not logged in yet");
           
       return obj.toJSONString();
	} 
    
    @SuppressWarnings("unchecked")
    public static String createActivityMessage(Command activityMessage, String username, String secret,
			JSONObject activity) {
    	JSONObject obj = new JSONObject();
        obj.put("command", Command.ACTIVITY_MESSAGE.toString());
        obj.put("username", username);
        obj.put("secret", secret);
        obj.put("activity", activity);
        return obj.toJSONString();
	}
    
    @SuppressWarnings("unchecked")
	public static String createActivityServerBroadcast(Message msg) {
		JSONObject obj = new JSONObject();
        obj.put("command", Command.ACTIVITY_SERVER_BROADCAST.toString());
        obj.put("activity", msg.getActivity());
        obj.put("timestamp", msg.getTimeStamp());
        obj.put("sender_ip_address", msg.getSenderIp());
        obj.put("sender_port_num", msg.getPortNum());
        return obj.toJSONString();
		
	} 
    
    @SuppressWarnings("unchecked")
    public static String createActivityBroadcast(JSONObject activity) {
        JSONObject obj = new JSONObject();
        obj.put("command", Command.ACTIVITY_BROADCAST.toString());
        obj.put("activity", activity);
       return obj.toJSONString();
        
    }  
    
    @SuppressWarnings("unchecked")
    public static String createActivityBroadcast(Message msg) {
        JSONObject obj = new JSONObject();
        obj.put("command", Command.ACTIVITY_BROADCAST.toString());
        obj.put("activity", msg.getActivity());
        return obj.toJSONString();
        
    } 
    
    @SuppressWarnings("unchecked")
    public static String createActivityAcknowledgemnt(long timestamp, String uniqueId){
        JSONObject obj = new JSONObject();
        String senderIp = uniqueId.split(" ")[0];
        String portNum = uniqueId.split(" ")[1];
        obj.put("command", Command.ACTIVITY_ACKNOWLEDGEMENT.toString());
        obj.put("timestamp", timestamp);
        obj.put("sender_ip_address", senderIp);
        obj.put("sender_port_num", portNum); 
        return obj.toJSONString();
    }
    
    @SuppressWarnings("unchecked")
    public static String usersRegisteredList(ArrayList<User> localUserList){
    	Gson gson = new GsonBuilder().create();
        String obj = gson.toJson(new UserListRequest(Command.USERS_REGISTERED_LIST.toString(),localUserList));
        return obj;
    }
    
    //The following methods are used to check if a command message is in a right format
    //For Register, Lock_Request, Lock_Denied, Lock_Allowed, Login and Register_Success_Broadcast
    public static boolean checkValidCommandFormat1(JSONObject obj){
        if (obj.containsKey("command")&& obj.containsKey("username")&&obj.containsKey("secret")){
            return true;
        }
        return false;
    }
    
    //For Invalid_Message, Authentication_Failed, Login_Success, Login_Failed, Register_Failed, Register_Success
    public static boolean checkValidCommandFormat2(JSONObject obj){
        if (obj.containsKey("command")&& obj.containsKey("info")){
            return true;
        }
        return false;
    }
    //For anonymous login
    public static boolean checkValidAnonyLogin(JSONObject obj){
        if (obj.containsKey("command")&& obj.containsKey("username")){
            if (obj.get("username").equals("anonymous")){
                return true;
            }
        }
        return false;
    }
    
    //For logout
    public static boolean checkValidLogout(JSONObject obj){
        if (obj.containsKey("command")){
            return true;
        }
        return false;
    }
    
    //For Authenticate
    public static boolean checkValidAuthenticate(JSONObject obj){
        if (obj.containsKey("command")&& obj.containsKey("secret")){
            return true;
        }
        return false;
    }
    
    //For Redirect. Also need to check whether it is a valid hostname&portnum
    public static boolean checkValidRedirect(JSONObject obj){
        if (obj.containsKey("command")&& obj.containsKey("hostname")&& obj.containsKey("port")){
            return true;
        }
        return false;
    }
    
    //For Activity_Message
    public static boolean checkValidAcitivityMessage(JSONObject obj){
        if (obj.containsKey("command")&& obj.containsKey("username")&& obj.containsKey("secret")&& obj.containsKey("activity")){
            return true;
        }
        return false;
    }
    
    //For server_announce
    public static boolean checkValidServerAnnounce(JSONObject obj){
        if (obj.containsKey("command")&& obj.containsKey("id")&& obj.containsKey("load")&& obj.containsKey("hostname")&& obj.containsKey("port")){
            return true;
        }
        return false;
    }
    
    //For Activity_broadcast
    public static boolean checkValidActivityBroadcast(JSONObject obj){
        if (obj.containsKey("command")&& obj.containsKey("activity")){
            return true;
        }
        return false;
    }
    
    //For Activity_acknowledgement
    public static boolean checkValidActivityAcknowledgment(JSONObject obj){
        if (obj.containsKey("command")&& obj.containsKey("timestamp")&& 
                obj.containsKey("sender_ip_address")&& obj.containsKey("sender_port_num")){
            return true;
        }
        return false;
    }
    
    //For Activity_Server_Broadcast
    public static boolean checkValidActivityServerBroadcast(JSONObject obj){
        if (obj.containsKey("command")&& obj.containsKey("activity")&& obj.containsKey("timestamp")&& 
                obj.containsKey("sender_ip_address")&& obj.containsKey("sender_port_num")){
            return true;
        }
        return false;
    }
    
    //For Sending the current registered user list
    public static boolean checkUsersRegisteredList(JSONObject obj){
        if (obj.containsKey("command")&& obj.containsKey("user_list")){
            return true;
        }
        return false;
    }
    
  //For Sending relay_message
    public static boolean checkRelayMessage(JSONObject obj){
        if (obj.containsKey("command")&& obj.containsKey("target_ip_address")&& obj.containsKey("target_port_num")&& obj.containsKey("relay_message")){
            return true;
        }
        return false;
    }
    
  //For Sending lock_request
    public static boolean checkLockRequest(JSONObject obj){
        if (obj.containsKey("command")&& obj.containsKey("username")&& obj.containsKey("secret")&& obj.containsKey("sender_ip_address")
                && obj.containsKey("sender_port_num")){
            return true;
        }
        return false;
    }
}
