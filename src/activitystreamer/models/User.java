package activitystreamer.models;

public class User {
	private String userName;
	private String secret;
	
	
	public User(String username, String secret) {
		this.userName = username;
		this.secret = secret;
	}
	
	public String getUsername() {
		return userName;
	}
	public void setUser(String user) {
		this.userName = user;
	}
	public String getSecret() {
		return secret;
	}
	public void setSecret(String secret) {
		this.secret = secret;
	}
	
	
	@Override
	public String toString() {
		return "User [userName=" + userName + ", secret=" + secret
				+ "]";
	}
	public boolean equals(User otherUser){
	    if(this.userName.equals(otherUser.getUsername()) && this.secret.equals(otherUser.getSecret())){
	        return true;
	    }
	    return false;
	}
	
	
}
