package com.acneuro.govner.jgroups;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import org.apache.commons.lang3.StringUtils;
import org.jgroups.Channel;
import org.jgroups.JChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseHAController {

    private boolean coordinator = false;
	
    private Logger log = LoggerFactory.getLogger(this.getClass());
    
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
    	
    	if(StringUtils.isBlank(System.getProperty("cluster_id"))){
    		return "jgroups-govner"; 
    	}
        return "jgroups-" + System.getProperty("cluster_id");
    }
    
    protected abstract JChannel connectToChannel(InputStream config,String configName);

	protected Channel init(){
        String config = System.getProperty("jgroups.config.xml");
        if (config == null || config.isEmpty()) {
        	config = "udp.xml";
        } 
              
        try {				
        	File configDir = new File(System.getProperty("catalina.base"), "shared");
        	File configFile = new File(configDir, config);
        	InputStream stream = new FileInputStream(configFile);
        	return connectToChannel(stream,null);        		
        } catch (Exception e) {
        	log.info("No file under tomcat /shared directory it will try to read from classpath");
        	return connectToChannel(null,config);
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
