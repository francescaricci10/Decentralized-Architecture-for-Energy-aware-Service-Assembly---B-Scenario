package lnu.mida.protocol;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.lajv.location.Location;
import lnu.mida.entity.GeneralNode;
import lnu.mida.entity.Service;
import peersim.cdsim.CDProtocol;
import peersim.cdsim.CDState;
import peersim.config.Configuration;
import peersim.core.Cleanable;
import peersim.core.CommonState;
import peersim.core.Network;
import peersim.core.Node;

public class OverloadApplication implements CDProtocol, Cleanable {

	// ///////////////////////////////////////////////////////////////////////
	// Constants
	// ///////////////////////////////////////////////////////////////////////

	/**
	 * The component assambly protocol.
	 */
	private static final String COMP_PROT = "comp_prot";

	/**
	 * The strategy
	 */
	private static String STRATEGY = "";

	// ///////////////////////////////////////////////////////////////////////
	// Fields
	// ///////////////////////////////////////////////////////////////////////

	/**
	 * The component assembly protocol id.
	 */
	private final int component_assembly_pid;


	/**
	 * Initialize this object by reading configuration parameters.
	 * 
	 * @param prefix the configuration prefix for this class.
	 */
	public OverloadApplication(String prefix) {
		super();
		STRATEGY = Configuration.getString("STRATEGY", "no strat");
		component_assembly_pid = Configuration.getPid(prefix + "." + COMP_PROT);
	}



	/**
	 * Makes a copy of this object. Needs to be explicitly defined, since we have
	 * array members.
	 */
	@Override
	public Object clone() {
		OverloadApplication result = null;
		try {
			result = (OverloadApplication) super.clone();
		} catch (CloneNotSupportedException ex) {
			System.out.println(ex.getMessage());
			assert (false);
		}
		return result;
	}

	// returns true if comp > old
	public Service chooseByStrategy(LinkedList<Service> candidates, GeneralNode node) {


		// random strategy
		if (STRATEGY.equals("random")) {
			return chooseByRandomStrategy(candidates);
		}
		if (STRATEGY.equals("weighted_random")) {
			return chooseByWeightedRandomStrategy(candidates);
		}
		// individual energy
		if (STRATEGY.equals("local_energy")) {
			return chooseByLocalEnergyStrategy(candidates, node);
		}
		// overall energy
		if (STRATEGY.equals("overall_energy")) {
			return chooseByOverallEnergyStrategy(candidates);
		}
		
		if (STRATEGY.equals("residual_life")) {
			return chooseByResidualLifeStrategy(candidates);
		}
		if (STRATEGY.equals("rev_residual_life")) {
			return chooseByRevResidualLifeStrategy(candidates);
		}
		if (STRATEGY.equals("latency_set")) {
			return chooseByLatencySetStrategy(candidates, node);
		}

		
		// exception is raised if a strategy is not selected
		else {
			try {
				throw new Exception("Strategy not selected");
			} catch (Exception e) {
				e.printStackTrace();
			}
			System.exit(0);
			return null;
		}
	}

	
	/*
	 * Seleziona il servizio allocato sul nodo con vita residua maggiore
	 * */
	private Service chooseByResidualLifeStrategy(LinkedList<Service> candidates) {
		
		if(CDState.getCycle()<7)
			return chooseByRandomStrategy(candidates);
		
		
		double max = 0;
		Service res = null;
		for(int i=0; i<candidates.size(); i++) {
			GeneralNode n = GeneralNode.getNode(candidates.get(i).getNode_id());
			if(n.getResidualLife()>max) {
				max = n.getResidualLife();
				res = candidates.get(i);
			}
		}
		
		
		if(res==null)
			return chooseByRandomStrategy(candidates);
		
		return res;
	}
	
	
	
	/*
	 * Seleziona il servizio allocato sul nodo con vita residua minore
	 * */
	private Service chooseByRevResidualLifeStrategy(LinkedList<Service> candidates) {
		
		if(CDState.getCycle()<7)
			return chooseByRandomStrategy(candidates);
		
		
		double min = Double.MAX_VALUE;
		Service res = null;
		for(int i=0; i<candidates.size(); i++) {
			GeneralNode n = GeneralNode.getNode(candidates.get(i).getNode_id());
			
			if(n.getResidualLife()<min) {
				min = n.getResidualLife();
				res = candidates.get(i);
			}
		}
	

		if(res==null)
			return chooseByRandomStrategy(candidates);
				
		return res;
	}


	// chooses a random component
	public Service chooseByRandomStrategy(LinkedList<Service> candidates) {
		
		int index = CommonState.r.nextInt(candidates.size());
		return candidates.get( index);
	}
	
	/*
	 * Seleziona il servizio in modo probabilistico in base alle vite residue dei nodi
	 * */
	private Service chooseByWeightedRandomStrategy(LinkedList<Service> candidates) {
		
		if(CDState.getCycle()<7)
			return chooseByRandomStrategy(candidates);	
				
		double sum=0;
		double finite_sum=0;
		
		ArrayList<Double> array = new ArrayList<Double>();
		
		for(int i=0; i<candidates.size(); i++) {

			if(candidates.get(i).getWeight()<Double.POSITIVE_INFINITY) {
				finite_sum+=candidates.get(i).getWeight();
			}
		}	
		
		for(int i=0; i<candidates.size(); i++) {

			if(candidates.get(i).getWeight()<Double.POSITIVE_INFINITY) {
				sum+=candidates.get(i).getWeight();
				array.add(sum);	
			}else {
				sum+=finite_sum;
				array.add(sum);	
			}
		}
		
		
		double max = 0;
		double min = sum;
		
		double random_num = min + (max - min) * CommonState.r.nextDouble();

		int index=0;
		
		for(int i=0; i<array.size(); i++) {
			if(array.get(i)>random_num) {
				index = i;
				break;
			}
		}	
		
		return candidates.get(index);
	}

	
	

	
	

	// local energy strategy
	private Service chooseByLocalEnergyStrategy(LinkedList<Service> candidates, GeneralNode node) {

		if(CDState.getCycle()<7)
			return chooseByRandomStrategy(candidates);
		
		Location thisLoc = node.getLocation();

		double min=Double.MAX_VALUE;
		Service res = null;
		
		
		for(int i=0; i<candidates.size(); i++) {
			
			GeneralNode other_node = GeneralNode.getNode(candidates.get(i).getNode_id());
			double energy = 0;

			if (node.getID() == other_node.getID()) {
				
				energy += candidates.get(i).getL_comp();

				// per il modello energetico adottato ad ECSA L_comm non dipende dal nodo in
				// ricezione (i.e., node)
				energy += candidates.get(i).getL_comm();
			} else {
				
				Location other_loc = other_node.getLocation();
				double other_latency = thisLoc.latency(other_loc);

				energy += node.getConsumedIndividualCommEnergySending(1, other_latency);
			}
			
			if(energy<min) {
				min=energy;
				res=candidates.get(i);
			}
		}
		

		return res;
		
	}
	
	

	// overall energy strategy
	private Service chooseByOverallEnergyStrategy(LinkedList<Service> candidates) {

		// at round 1 the overall energy is not known
		if(CDState.getCycle()<7)
			return chooseByRandomStrategy(candidates);
				
		double min=Double.MAX_VALUE;
		Service res = null;
		
		for(int i=0; i<candidates.size(); i++) {

			double energy = candidates.get(i).getE_comp() + candidates.get(i).getE_comm();

			if(energy<min) {
				min=energy;
				res=candidates.get(i);
			}	
		}
		
		return res;
	}

	
	
	


	
	
	private Service chooseByLatencySetStrategy(LinkedList<Service> candidates, GeneralNode node) {
		
		if(CDState.getCycle()<7)
			return chooseByRandomStrategy(candidates);
		
		
		double max = 0;
		double max_best = 0;
		Service res = null;
		Service res_best = null;
		for(int i=0; i<candidates.size(); i++) {
			GeneralNode n = GeneralNode.getNode(candidates.get(i).getNode_id());
			if(n.getBestNode()) {
				if (n.inPeerSet(node)) {
					if(n.getResidualLife()>max_best) {
						max_best = n.getResidualLife();
						res_best = candidates.get(i);
					}
				}
			}else {
				if(n.getResidualLife()>max) {
					max = n.getResidualLife();
					res = candidates.get(i);
				}
			}
			
			
		}
		
		if(res==null&&res_best==null)
			return chooseByRandomStrategy(candidates);
		
		if(res_best!=null)
			return res_best;
		return res;
	}
	
	
	

	@Override
	public void onKill() {
		// TODO Auto-generated method stub
	}

	@Override
	public void nextCycle(Node node, int protocolID) {
	}


}
