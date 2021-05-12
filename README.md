# Decentralized-Architecture-for-Energy-aware-Service-Assembly---B-Scenario
This repository contains the simulation code that implements a decentralized architecture to manage an energy-aware service assembly.
This project extends the project predented in https://github.com/mi-da/Energy-Aware-Service-Assembly-Journal.

We suppose that each node of the network is powered by a non-rechargeable battery.

## **Instructions**

- Download the Java Project "Decentralized-Architecture-for-Energy-aware-Service-Assembly---B-Scenario" and import it in your IDE as a Java project
- Link the provided libraries in "ext-lib" to the project
- Input the program argument "configs/mida-assembly-config.txt" (i.e., the configuration file of PeerSim)
- The main class to run the experiments is "peersim.Simulator"

## **Configuration Parameters**

The file configs/mida-assembly-config.txt contains the configuration parameters for the simulation. The main parameters are:

NETWORK_SIZE: The number of services to assemble
TYPES: Number of types of services
STRATEGY: The selection criteria that the nodes adopt. We implemented multiple strategies:
  1) random 
  2) weighted random
  3) residual_life
  4) reverse residual life
  5) local energy
  6) overall energy
  7) latency set

## **New Features**

The main new features of this project are organized as follow:
- the initial settings of the battery are placed in src/lnu/mida/controller/init/OverloadComponentInitializer.java
- the update of the battery status is done in src/lnu/mida/controller/energy/EnergyController.java
- the controller src/lnu/mida/controller/LinkController.java manages the bindings between services at the end of the gossiping procedure
- in src / lnu / mida / controller / OverloadReset.java we check the status of the nodes: nodes with no battery die (the node is set as unavailable and every link of its services is removed)
