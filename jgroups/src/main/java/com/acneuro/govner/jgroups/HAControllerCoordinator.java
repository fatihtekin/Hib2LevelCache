package com.acneuro.govner.jgroups;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.jgroups.Address;
import org.jgroups.Channel;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.jgroups.View;
import org.jgroups.protocols.pbcast.GMS;

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
public final class HAControllerCoordinator {

    private static HAControllerCoordinator me = new HAControllerCoordinator();
    private java.util.logging.Logger log = java.util.logging.Logger.getLogger("groups");

    public static void main(String[] args) throws Exception {
                
        Thread.sleep(60000);
    }
    
    public static HAControllerCoordinator getInstance() {
        return me;
    }

    private HAControllerCoordinator() {
        String config = System.getProperty("jgroups.config.xml");
        if (config == null || config.isEmpty()) {
            // we will always be the coordinator, i.e., not using clustering for H/A.
            setCoordinator(true);
        } else {
            JChannel channel = connectToChannel(config);
            if(!coordinator){            	
            	try {
            		Address src = channel.getAddress();
            		Address dst = channel.getView().getViewId().getCreator();
            		channel.send(dst , ("Whatsup from "+channel.getAddress()).getBytes());
            	} catch (Exception e) {
            		// TODO Auto-generated catch block
            		e.printStackTrace();
            	}
            }
            changeView(channel);
        }
    }

    
    protected void changeView(Channel ch) {
        View view=ch.getView();
        Address local_addr=ch.getAddress();
        Address coord=view.getMembersRaw()[0];
//        if(!local_addr.equals(coord)) {
//            System.err.println("View can only be changed on coordinator");
//            return;
//        }
        if(view.size() == 1) {
            System.err.println("Coordinator cannot change as view only has a single member");
            return;
        }
        List<Address> mbrs=new ArrayList<Address>(view.getMembers());
        long new_id=view.getViewId().getId() + 1;
        
        Address tmp_coord=mbrs.remove(0);
        mbrs.add(tmp_coord);
        View new_view=new View(ch.getAddress(), new_id, mbrs);
        GMS gms=(GMS)ch.getProtocolStack().findProtocol(GMS.class);
        gms.castViewChange(new_view, null, mbrs);
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

                private boolean coordinating(View view) {
                	return (channel.getAddress().equals(view.getViewId().getCreator()));
                }

                public void viewAccepted(View view) {
                    if (coordinating(view)) {
                        if (currentCoordinator == null ||
                                !currentCoordinator.equals(view.getViewId().getCreator())) {
                            currentCoordinator = view.getViewId().getCreator();
                            log.info("I am the NEW coordinator: " + channel.getAddress());
                            setCoordinator(true);
                        }
                    }else{
                        log.info("Not the coordinator : "+ channel.getAddress());
                    }
                }

                public void receive(Message msg) {
                    log.log(Level.INFO,"received msg from " + msg.getSrc() + ": " + new String(msg.getBuffer()));
                }
            });
            String clusterName = getClusterName();
            channel.connect(clusterName);
            channelToReturn = channel;
        } catch (Exception e) {
            // we will never be the coordinator.
            // The problem must be fixed
            // and the application must be restarted.
            log.log(Level.SEVERE,"", e);
        }
        return channelToReturn;
    }

}
