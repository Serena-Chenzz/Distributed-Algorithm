package activitystreamer.server.commands;
//Called when a Prepare message is received from a Proposer
public class Prepare {

	Integer proposalID, promisedID,acceptedID;
	Object acceptedValue;
	
	public Prepare(String msg, Connection con) {
		
		this.proposalID = proposalID;
		Acceptor acceptor = new Acceptor();
		this.promisedID = acceptor.promisedID;
		this.acceptedID = acceptor.acceptedID;
		this.acceptedValue = acceptor.acceptedValue;
		
		if (promisedID != null && proposalID.equals(promisedID)) { // it is a duplicate message
			sendPromise.sendPromise(proposalID, acceptedID, acceptedValue);
		}
		else if (promisedID == null || proposalID > promisedID) { // it is greater than promisedID, then change the promisedID
			promisedID = proposalID;
			sendPromise.sendPromise(proposalID, acceptedID, acceptedValue);
		}
		else {
			sendNack.sendNack(proposalID);
		}

	}
	
	public void sendPromise(Integer proposalID, Integer acceptedID, Object acceptedValue) {
			
			Proposer proposer = new Proposer();	    
			proposer.proposalID = proposalID;
			proposer.acceptedID = acceptedID;
			proposer.acceptedValue = acceptedValue;
		}
	
	public static void sendNack(Integer proposalID) {
		Proposer proposer = new Proposer();
		proposer.proposalID = proposalID;
		
	}


}
