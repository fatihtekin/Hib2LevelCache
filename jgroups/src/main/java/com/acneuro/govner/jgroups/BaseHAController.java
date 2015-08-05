package com.acneuro.govner.jgroups;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import org.jgroups.Channel;
import org.jgroups.JChannel;

public abstract class BaseHAController {

    private boolean coordinator = false;
	
    /**
     * @return TRUE if we are the coordinator.
     */
    public boolean isCoordinator() {
        return coordinator;
    }


    protected synchronized void setCoordinator(boolean coordinator) {
        this.coordinator = coordinator;
    }

    public String getClusterName() {
        return "jgroups-" + Long.parseLong(System.getProperty("cluster_id"));
    }
    
    protected abstract JChannel connectToChannel(InputStream config,String configName);

	protected Channel init(){
        String config = System.getProperty("jgroups.config.xml");
        if (config == null || config.isEmpty()) {
        	return null;
        } else {
        	try {				
        		File configDir = new File(System.getProperty("catalina.base"), "shared");
        		File configFile = new File(configDir, config);
        		InputStream stream = new FileInputStream(configFile);
				return connectToChannel(stream,null);        		
        	} catch (Exception e) {
				return connectToChannel(null,config);
			}

        }

	}
	
    protected JChannel createChannel(InputStream config, String configName)
            throws Exception {
        if(configName != null) {
            return new JChannel(configName);
        }
        return new JChannel(config);
    }
    
}
