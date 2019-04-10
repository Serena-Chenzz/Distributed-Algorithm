package activitystreamer.server.commands;

import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import activitystreamer.models.Command;
import activitystreamer.models.User;
import activitystreamer.server.Control;

public class UserListBroadCastThread extends Thread {

	private static boolean closeConnection=false;
    private final static Logger log = LogManager.getLogger();
    
    public UserListBroadCastThread() {
        start();
    }
    
    @Override
    public void run() {
        log.info("UserListBroadCastThread is running");
        while(!Control.getInstance().getTerm()){
            //Fetch clientMsgBufferQueue
            try{
                ArrayList<User> registeredUsers = Control.getLocalUserList();
                String registerUserListMsg = Command.usersRegisteredList(registeredUsers);
                Control.getInstance().broadcast(registerUserListMsg);
                Thread.sleep(3000);
            }catch (InterruptedException e){
                log.info("This thread is interrupted forcefully.");
            }
        }
        
        log.info("closing broadcasing userList thread....");
        closeConnection=true;
    }
    
    public static boolean getResponse() {
        return closeConnection;
    }
}
