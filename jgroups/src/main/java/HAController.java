import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import org.jgroups.*;
import org.jgroups.stack.IpAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HA controller, using JGroups, detects if we are the coordinator in the cluster.
 * <p>
 * The cluster name is in the form: event-engine-cluster-${engine.slave.id}
 * </p>
 * <p>
 * Which jgroups configuration to use is decided through java system property. Something like:
 * <code>-Djgroups.config.xml=tcp.xml</code>
 * And then, you have to look at the documentation of the related xml file in jgroups distribution
 * to see which additional runtime parameters you have to provide.
 * </p>
 *
 */
public final class HAController {

    private static HAController me = new HAController();
    private Logger log = LoggerFactory.getLogger("groups");

    public static void main(String[] args) throws Exception {
                
        Thread.sleep(60000);
    }
    
    public static HAController getInstance() {
        return me;
    }

    private HAController() {
        String config = System.getProperty("jgroups.config.xml");
        if (config == null || config.isEmpty()) {
            // we will always be the coordinator, i.e., not using clustering for H/A.
            setCoordinator(true);
        } else {
            JChannel channel = connectToChannel(config);
           // if(coordinator){            	
            	try {
            		while(true){            			
            			if(channel.getView().getMembers().size() > 1){            				
            				List<Address> dsts = channel.getView().getMembers();
            				for (Address dst : dsts) {
            					if(!channel.getAddress().equals(dst))
            					channel.send(dst , ("GiveMeYourHostnameAndPort").getBytes());
							}
            			}
            			Thread.sleep(5000);
            		}
            	} catch (Exception e) {
            		// TODO Auto-generated catch block
            		e.printStackTrace();
            	}
        //    }
        }
    }

    private boolean coordinator = false;

    private synchronized void setCoordinator(boolean coordinator) {
        this.coordinator = coordinator;
    }

    /**
     * @return TRUE if we are the coordinator.
     */
    public boolean isCoordinator() {
        return coordinator;
    }

    private String getClusterName() {
        return "event-engine-cluster-" + Long.parseLong(System.getProperty("cluster_id"));
    }

    /**
     * @param config A string that is acceptable for the JChannel configuration property.
     * @see org.jgroups.JChannel#JChannel(String)
     */
    private JChannel connectToChannel(String config) {
        JChannel channelToReturn = null;
        try {
            final JChannel channel = new JChannel(config);
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
                    		log.info("Node has dropped ip:"+ srcIp);
                    	}

                    }
                    
                    currentMembers.removeAll(addresses);
                    for (Address newlyJoinedNode : currentMembers) {
                    	PhysicalAddress physicalAddr = (PhysicalAddress)channel.down(new Event(Event.GET_PHYSICAL_ADDRESS, newlyJoinedNode));
                    	if(physicalAddr instanceof IpAddress) {
                    		IpAddress ipAddr = (IpAddress)physicalAddr;
                    		String srcIp = ipAddr.getIpAddress().getHostAddress();
                    		log.info("Node has just joined ip:"+ srcIp);
                    	}
                    }
                    addresses = new ArrayList<Address>(view.getMembers());
                    
                }

                
                
                public void receive(Message msg) {
                    log.info("received msg from " + msg.getSrc() + ": " + new String(msg.getBuffer()));					
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

}
