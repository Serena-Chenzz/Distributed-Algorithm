package activitystreamer.models;

public class Acceptor {
	protected Integer  promisedID = null;
	protected Integer  acceptedID = null;
	protected Object   acceptedValue = null; 
	
	receivePrepare pre = new receivePrepare(Proposer.proposalID);
	receiveAcceptReq accReq = new receiveAcceptReq(Proposer.proposalID);
	

	
	public Integer getPromisedID() {
		return promisedID;
	}

	public Integer getAcceptedID() {
		return acceptedID;
	}

	public Object getAcceptedValue() {
		return acceptedValue;
	}

}
