package activitystreamer.server.commands;

//Called when an Accept message is received from a Proposer
public class Accept {

	Integer proposalID, promisedID,acceptedID;
	Object acceptedValue;
	
	public Accept(Integer proposalID, Object value) {
		
		this.proposalID = proposalID;
		Acceptor acceptor = new Acceptor();
		this.promisedID = acceptor.promisedID;
		this.acceptedID = acceptor.acceptedID;
		this.acceptedValue = acceptor.acceptedValue;
		
		if (promisedID == null || proposalID > promisedID
				|| proposalID.equals(promisedID)) { 
			promisedID = proposalID;
			acceptedID = proposalID;
			acceptedValue = value;
			
			
		}
		else {
			sendNack.sendNack(proposalID);
		}
	}
	
	public static void sendAccepted(Integer proposalID) {
		Proposer proposer = new Proposer();
		proposer.acceptedID = proposalID;
	}
	
	public static void sendNack(Integer proposalID) {
		Proposer proposer = new Proposer();
		proposer.proposalID = proposalID;
		
	}
}
