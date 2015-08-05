package com.acneuro.govner.jgroups;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.jgroups.Address;
import org.jgroups.Event;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.PhysicalAddress;
import org.jgroups.ReceiverAdapter;
import org.jgroups.View;
import org.jgroups.stack.IpAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public final class HAController extends BaseHAController{

    private static HAController me = new HAController();
    private Logger log = LoggerFactory.getLogger(this.getClass());
    private List<Observer> observers = new ArrayList<Observer>();
    
    private static Set<Node> nodes;
    
    public synchronized void attach(Observer observer){
        observers.add(observer);
    	if(nodes.size() > 0){
    		observer.notifyNodesJoin(nodes);
    	}
    }

    private void notifyObserversForJoin(JChannel channel,List<Address> joinedAddresses,String contextPath){
    	
    	if(joinedAddresses.size() == 0){
    		return;
    	}
    	
    	Set<Node> joinedNodes = new HashSet<Node>();
    	for (Address joinedAddress : joinedAddresses) {			
    		Node joinedNode = createNodeInfoFromAddress(channel, contextPath,joinedAddress,true);
    		joinedNodes.add(joinedNode);
		}
    	
    	nodes.addAll(joinedNodes);
    	
    	for (Observer observer : observers) {
    		observer.notifyNodesJoin(joinedNodes);
    	}
    }

    
    private void notifyObserversForLeave(JChannel channel,List<Address> leftAddresses){
    	
    	if(leftAddresses.size() == 0){
    		return;
    	}
    	Set<Node> leftNodes = new HashSet<Node>();
    	for (Address leftAddress : leftAddresses) {			
    		
    		Node leftNode = createNodeInfoFromAddress(channel, null,leftAddress,false);
    		leftNodes.add(leftNode);
		}
    	nodes.removeAll(leftNodes);

    	for (Observer observer : observers) {
    		observer.notifyNodesLeave(leftNodes);
    	}
    } 
    
    
	private Node createNodeInfoFromAddress(JChannel channel,
			String contextPath, Address joinedAddress,boolean isJoin) {
		PhysicalAddress physicalAddr = (PhysicalAddress)channel.down(new Event(Event.GET_PHYSICAL_ADDRESS, joinedAddress));
		String srcIp = "";
		String hostName = "";
		if(physicalAddr instanceof IpAddress) {
			IpAddress ipAddr = (IpAddress)physicalAddr;
			srcIp = ipAddr.getIpAddress().getHostAddress();
			hostName = ipAddr.getIpAddress().getHostName();
			if(isJoin){				
				log.debug("Node has joined ip:{} hostname:{} nodeId:{}" , srcIp,hostName,joinedAddress.toString());
			}else{
				log.debug("Node has left ip:{} hostname:{} nodeId:{}" , srcIp,hostName,joinedAddress.toString());				
			}
		}
		
		Node node = Node.builder().contextPath(contextPath).ip(srcIp).hostName(hostName).nodeId(joinedAddress.toString()).build();
		return node;
	} 
    
    



     
    public static void main(String[] args) throws Exception {
                
        Thread.sleep(60000);
    }
    
    public static HAController getInstance() {
        return me;
    }

    private HAController() {
    	nodes = Collections.synchronizedSet(new HashSet<Node>());
		JChannel channel = (JChannel) init();
    }


    protected JChannel connectToChannel(InputStream config,String configName) {
        JChannel channelToReturn = null;
        try {
            final JChannel channel = createChannel(config, configName);
            channel.setReceiver(new ReceiverAdapter() {
                Address currentCoordinator = null;
                List<Address> addresses = new ArrayList<Address>();
                private boolean coordinating(View view) {
                	return (channel.getAddress().equals(view.getViewId().getCreator()));
                }

                public void viewAccepted(View view) {
                    if (coordinating(view)) {
                        if (currentCoordinator == null ||
                                !currentCoordinator.equals(view.getMembers().get(0))) {
                            currentCoordinator = view.getMembers().get(0);
                            log.info("I am the NEW coordinator: " + channel.getAddress());
                            setCoordinator(true);
                        }
                    }else{
                        log.info("Not the coordinator : "+ channel.getAddress());
                    }
                    List<Address> currentMembers =  new ArrayList<Address>(view.getMembers());
                    List<Address> previousMembers =  new ArrayList<Address>(addresses);

                    previousMembers.removeAll(currentMembers);
                    for (Address droppedNode : previousMembers) {
                    	PhysicalAddress physicalAddr = (PhysicalAddress)channel.down(new Event(Event.GET_PHYSICAL_ADDRESS, droppedNode));
                    	if(physicalAddr instanceof IpAddress) {
                    		IpAddress ipAddr = (IpAddress)physicalAddr;
                    		String srcIp = ipAddr.getIpAddress().getHostAddress();
                    		log.info("Node has dropped ip:{} node-id:{}", srcIp,droppedNode.toString());
                    	}

                    }
                    
                    notifyObserversForLeave(channel, previousMembers);
                    
                    currentMembers.removeAll(addresses);
                    for (Address newlyJoinedNode : currentMembers) {
                    	PhysicalAddress physicalAddr = (PhysicalAddress)channel.down(new Event(Event.GET_PHYSICAL_ADDRESS, newlyJoinedNode));
                    	if(physicalAddr instanceof IpAddress) {
                    		IpAddress ipAddr = (IpAddress)physicalAddr;
                    		String srcIp = ipAddr.getIpAddress().getHostAddress();
                    		log.info("Node has just joined ip:{} node-id:{}", srcIp,newlyJoinedNode.toString());
                    		if(!channel.getAddress().equals(newlyJoinedNode)){
                    			try{                    				
                    				channel.send(newlyJoinedNode , ("GiveMeYourHostnameAndPort").getBytes());
                    			}catch(Exception e){
                    				log.error("Could not send GiveMeYourHostnameAndPort command to ",newlyJoinedNode.toString());
                    			}
                    		}
                    	}
                    }
                                        
                    addresses = new ArrayList<Address>(view.getMembers());
                    
                }

                public void receive(Message msg) {
                    String contextPath = new String(msg.getBuffer());
					log.info("received msg from " + msg.getSrc() + ": " + contextPath);
					List<Address> joinedAddresses = new LinkedList<Address>();
					joinedAddresses.add(msg.getSrc());
                    notifyObserversForJoin(channel, joinedAddresses, contextPath);
                    					
                }
            });
            String clusterName = getClusterName();
            channel.connect(clusterName);
            channelToReturn = channel;
        } catch (Exception e) {
            // we will never be the coordinator.
            // The problem must be fixed
            // and the application must be restarted.
            log.error("", e);
        }
        return channelToReturn;
    }

    public static Set<Node> getNodes() {
		return new HashSet<Node>(nodes);
	}
}
