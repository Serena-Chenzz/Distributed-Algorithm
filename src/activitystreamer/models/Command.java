package activitystreamer.models;

import java.util.ArrayList;

import org.json.simple.JSONObject;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import activitystreamer.server.Message;


public enum Command {
    AUTHENTICATE, INVALID_MESSAGE, AUTHENTICATION_FAIL, AUTHENTICATION_SUCCESS, LOGIN, LOGIN_SUCCESS, 
    REDIRECT, LOGIN_FAILED, LOGOUT, ACTIVITY_MESSAGE, SERVER_ANNOUNCE,
    ACTIVITY_BROADCAST, REGISTER, REGISTER_FAILED, REGISTER_SUCCESS
    ;


    public static boolean contains(String commandName) {
        for (Command c : Command.values()) {
            if (c.name().equals(commandName)) {
                return true;
            }
        }
        return false;
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
    public static JSONObject createLogout(String username, String secret){
        JSONObject obj = new JSONObject();
        obj.put("command", LOGOUT.toString());
        obj.put("username", username);
        obj.put("secret", secret);
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
    
    @SuppressWarnings("unchecked")
    public static JSONObject createRegisterSuccess(String username){
        JSONObject obj = new JSONObject();
        obj.put("command", REGISTER_SUCCESS.toString());
        obj.put("info", "register success for " + username);
        
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
}
