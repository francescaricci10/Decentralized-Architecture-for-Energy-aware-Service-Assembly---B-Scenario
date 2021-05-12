# Decentralized-Architecture-for-Energy-aware-Service-Assembly---B-Scenario
This repository contains the simulation code that implements a decentralized architecture to manage an energy-aware service assembly. 
We suppose that each node of the network is powered by a non-rechargeable battery.

The main features of this project are organized as follow:
- the initial settings of the battery are placed in src/lnu/mida/controller/init/OverloadComponentInitializer.java
- the update of the battery status is done in src/lnu/mida/controller/energy/EnergyController.java
- the controller src/lnu/mida/controller/LinkController.java manages the bindings between services at the end of the gossiping procedure
- in src / lnu / mida / controller / OverloadReset.java we check the status of the nodes: nodes with no battery die (the node is set as unavailable and every link of its services is removed)
