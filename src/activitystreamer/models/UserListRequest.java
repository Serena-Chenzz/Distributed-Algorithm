package activitystreamer.models;

import java.util.ArrayList;

public class UserListRequest {

	String command;
	ArrayList<User> user_list;
	
	public UserListRequest(String command, ArrayList<User> userList) {
		super();
		this.command = command;
		this.user_list = userList;
	}
	public String getCommand() {
		return command;
	}
	public void setCommand(String command) {
		this.command = command;
	}
	public ArrayList<User> getUserList() {
		return user_list;
	}
	public void setUserList(ArrayList<User> userList) {
		this.user_list = userList;
	}
	
	
}
