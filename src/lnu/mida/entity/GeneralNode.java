package lnu.mida.entity;

import java.util.ArrayList;
import java.util.List;

import com.lajv.location.Location;

import peersim.config.*;
import peersim.core.Cleanable;
import peersim.core.CommonState;
import peersim.core.Fallible;
import peersim.core.Network;
import peersim.core.Node;
import peersim.core.Protocol;

/**
* This is the default {@link Node} class that is used to compose the
* {@link Network}.
*/
public class GeneralNode implements Node {


// ================= fields ========================================
// =================================================================

public Location location;
	

/** used to generate unique IDs */
public static long counterID = -1;

/**
* The protocols on this node.
*/
protected Protocol[] protocol = null;

/**
* The current index of this node in the node
* list of the {@link Network}. It can change any time.
* This is necessary to allow
* the implementation of efficient graph algorithms.
*/
private int index;

/**
* The fail state of the node.
*/
protected int failstate = Fallible.OK;

/**
* The ID of the node. It should be final, however it can't be final because
* clone must be able to set it.
*/
private long ID;


/** 
 * Energy related parameters
 */


// consumption rate of the node
private double R;

//battery 
private double Battery;

//initial capacity 
private double capacity;

//residual time of the battery
private double residual_life;

//set of the k nearest nodes 
private ArrayList<Node> peer_set;

//true if this is a "best node"
private boolean best_node;

private int link_num;
private int total_link_num;


/** 
 * set of services discovered by the node
 */
private CandidateServices candidate_services;



private double [] qos_ee_reputation;
private double [] qos_tk_reputation;
private int [] qos_reputation_counter;
private double [] qos_qk_reputation;

// Receiving costs 1 unit of energy = Eelect
//private double Eelect = 0.02;
private double Eelect = 0.000000005;
// Sending a message costs on average 2 times of Eelect: this leads to the definition of Eamp (average distance is 90 meters)
//private double Eamp  = 0.02/Math.pow(90, 2);
private double Eamp  = 0.000000000001;
// The standard cost of a CPU operation is equal to the maximum cost of sending a message
private double CPUCost= 0.000000005;
//private double CPUCost= 0.04;


// ================ constructor and initialization =================
// =================================================================

/** Used to construct the prototype node. This class currently does not
* have specific configuration parameters and so the parameter
* <code>prefix</code> is not used. It reads the protocol components
* (components that have type {@value peersim.core.Node#PAR_PROT}) from
* the configuration.
*/
public GeneralNode(String prefix) {
	location = (Location) Configuration.getInstance(prefix + "." + "loc_impl");
	String[] names = Configuration.getNames(PAR_PROT);
	
	CommonState.setNode(this);
	ID=nextID();
	protocol = new Protocol[names.length];
	for (int i=0; i < names.length; i++) {
		CommonState.setPid(i);
		Protocol p = (Protocol) 
			Configuration.getInstance(names[i]);
		protocol[i] = p; 
	}
	
}


// -----------------------------------------------------------------

@Override
public Object clone() {
	
	GeneralNode result = null;
	try { result=(GeneralNode)super.clone(); }
	catch( CloneNotSupportedException e ) {} // never happens
	result.protocol = new Protocol[protocol.length];
	CommonState.setNode(result);
	result.ID=nextID();
	for(int i=0; i<protocol.length; ++i) {
		CommonState.setPid(i);
		result.protocol[i] = (Protocol)protocol[i].clone();
	}
	result.location = (Location) location.clone();
	return result;
}

// -----------------------------------------------------------------

/** returns the next unique ID */
private long nextID() {

	return counterID++;
}

// =============== public methods ==================================
// =================================================================


@Override
public void setFailState(int failState) {
	
	// after a node is dead, all operations on it are errors by definition
	if(failstate==DEAD && failState!=DEAD) throw new IllegalStateException(
		"Cannot change fail state: node is already DEAD");
	switch(failState)
	{
		case OK:
			failstate=OK;
			break;
		case DEAD:
			//protocol = null;
			index = -1;
			failstate = DEAD;
			for(int i=0;i<protocol.length;++i)
				if(protocol[i] instanceof Cleanable)
					((Cleanable)protocol[i]).onKill();
			break;
		case DOWN:
			failstate = DOWN;
			break;
		default:
			throw new IllegalArgumentException(
				"failState="+failState);
	}
}

public Location getLocation() {
	return location;
}

// -----------------------------------------------------------------

@Override
public int getFailState() { return failstate; }

// ------------------------------------------------------------------

@Override
public boolean isUp() { return failstate==OK; }

// -----------------------------------------------------------------

@Override
public Protocol getProtocol(int i) { return protocol[i]; }

//------------------------------------------------------------------

@Override
public int protocolSize() { return protocol.length; }

//------------------------------------------------------------------

@Override
public int getIndex() { return index; }

//------------------------------------------------------------------

@Override
public void setIndex(int index) { this.index = index; }
	
//------------------------------------------------------------------

/**
* Returns the ID of this node. The IDs are generated using a counter
* (i.e. they are not random).
*/
@Override
public long getID() { return ID; }

//------------------------------------------------------------------

@Override
public String toString() 
{
	StringBuffer buffer = new StringBuffer();
	buffer.append("ID: "+ID+" index: "+index+"\n");
	for(int i=0; i<protocol.length; ++i)
	{
		buffer.append("protocol["+i+"]="+protocol[i]+"\n");
	}
	return buffer.toString();
}

//------------------------------------------------------------------

/** Implemented as <code>(int)getID()</code>. */
@Override
public int hashCode() { return (int)getID(); }


public static GeneralNode getNode(long id) {
	for (int i = 0; i < Network.size(); i++) {
		GeneralNode n = (GeneralNode) Network.get(i);
		if(n.getID()==id)
			return n;
	}
	try {
		throw new Exception("No node found with id "+id);
	} catch (Exception e) {
		e.printStackTrace();
	}
	System.exit(0);
	return null;
}

public void setCPUConsumptionFactor(double ampFactor) {
	this.CPUCost = CPUCost*ampFactor;
}

public void setCommunicationConsumptionFactor(double ampFactor) {
	this.Eelect = Eelect*ampFactor;
	this.Eamp = Eamp*ampFactor;
}


// returns individual CPU energy consumption
public double getConsumedIndividualCPUEnergy(double lambda_CPU) {	
//	System.out.println("cpu cost "+ lambda_CPU*CPUCost+" "+CPUCost);
	return lambda_CPU*CPUCost; // + (CPUCost/10);			
}

// return consumed individual communication energy consumption for sending
public double getConsumedIndividualCommEnergySending(double lambda, double latency) {
	
	// No communication involved for services on the same node
	if(latency==0)
		return 0;	
	
	//int k = 512; // packet size = 64 byte
	int k = 1264; // packet size = 158 byte
	//int k = 18432; // packet size = 158 byte
	
	// Sending energy =  K(E_{elect} + E_{amp} l_{S,S'}^2)
	double sendingEnergy = lambda*(k*(Eelect+(Eamp*Math.pow(latency,2))));
	
	
	//if(this.getID()==0)
	//System.out.println("	latency "+latency);
	//System.out.println("Eamp "+Eamp);
	//System.out.println("Eelect "+Eelect);
	//System.out.println("sending energy "+sendingEnergy);
	
	return sendingEnergy;	
}

// returns consumed individual communication energy consumption for receiving
public double  getConsumedIndividualCommEnergyReceiving(double lambda) {
	// Receiving energy = K(E_{elect})
	//int k = 512;
	int k = 1264;
	//int k = 18432;
	double receivingEnergy = lambda*k*Eelect;
	
//	System.out.println("lambda "+lambda);
//	System.out.println("receiving energy "+receivingEnergy);
	return receivingEnergy;	
}

public double getBattery() {
	return Battery;
}

public void setBattery(double battery) {
	Battery = battery;
}


public double getR() {
	return R;
}


public void setR(double r) {
	R = r;
}

public double getResidualLife() {
	return residual_life;
}


public void setResidualLife(double val) {
	residual_life = val;
}



public ArrayList<Node> getPeerSet() {
	return peer_set;
}

public void addPeerSet(Node node) {
	peer_set.add(node);
}

public int getPeerSetSize() {
	return peer_set.size();
}

public void resetPeerSet() {
	peer_set = new ArrayList<Node>();
}

public boolean inPeerSet(Node node) {
	
	for(int i=0; i<peer_set.size(); i++) {
		if(node==peer_set.get(i))
			return true;
	}
	return false;
}

public boolean getBestNode() {
	return best_node;
}

public void setBestNode(boolean b) {
	best_node = b;
}

public int getLinkNum() {
	return link_num;
}

public void setLinkNum(int val) {
	link_num = val;
}

public int getTotalLinkNum() {
	return total_link_num;
}

public void setTotalLinkNum(int val) {
	total_link_num += val;
}

public double getCapacity() {
	return capacity;
}

public void setCapacity(double val) {
	capacity = val;
}


public double getTk(long serviceID) {
	return qos_tk_reputation[(int) (serviceID%5)];
}
public double getEeQos(long serviceID) {
	return qos_ee_reputation[(int) (serviceID%5)];
}
public int getQosCounter(long serviceID) {
	return qos_reputation_counter[(int) (serviceID%5)];
}
public double getQk(long serviceID) {
	return qos_qk_reputation[(int) (serviceID%5)];
}

public void setTk(double val, long serviceID) {
	qos_tk_reputation[(int) (serviceID%5)] = val;
}
public void setEeQos(double val, long serviceID) {
	qos_ee_reputation[(int) (serviceID%5)] = val;
}
public void setQosCounter(int val, long serviceID) {
	qos_reputation_counter[(int) (serviceID%5)] = val;
}
public void setWindow(double val, long serviceID) {
	qos_qk_reputation[(int) (serviceID%5)] = val;
}

public CandidateServices getCandidateServices() {
	return candidate_services;
}

public void setCandidateServices(CandidateServices cs) {
	candidate_services = cs;
}

public void resetCandidateServices() {
	//candidate_services = new CandidateServices();
	candidate_services.clearLists();
}

public void setCandidateServicesID(long id) {
	//candidate_services = new CandidateServices();
	candidate_services.setNodeID(id);
}

public void initCand() {
	
	candidate_services = new CandidateServices();
	candidate_services.initializeLists();
	candidate_services.setNodeID(this.getID());
}




}


