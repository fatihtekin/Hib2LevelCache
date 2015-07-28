import org.apache.log4j.Logger;
import org.jgroups.*;

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
    private Logger log = Logger.getLogger("groups");

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
                    log.debug("received msg from " + msg.getSrc() + ": " + new String(msg.getBuffer()));
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
