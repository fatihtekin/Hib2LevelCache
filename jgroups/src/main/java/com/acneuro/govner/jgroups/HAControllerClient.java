package com.acneuro.govner.jgroups;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.InetAddress;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.catalina.ServerFactory;
import org.apache.commons.lang3.StringUtils;
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

public class HAControllerClient extends BaseHAController implements ServletContextListener{

    private Logger log = LoggerFactory.getLogger(HAControllerClient.class);
    
    private JChannel channel;

    /**
     * @param config A string that is acceptable for the JChannel configuration property.
     * @see org.jgroups.JChannel#JChannel(String)
     */
    protected JChannel connectToChannel(InputStream config,String configName) {
        JChannel channelToReturn = null;
        try {
            final JChannel channel = createChannel(config, configName);
            channel.setReceiver(new ReceiverAdapter() {
                Address currentCoordinator = null;

                private boolean coordinating(View view) {
                	return (channel.getAddress().equals(view.getViewId().getCreator()));
                }

                public void viewAccepted(View view) {
                    if (coordinating(view)) {
                        if (currentCoordinator == null ||
                                !currentCoordinator.equals(view.getViewId().getCreator())) {
                            currentCoordinator = view.getMembers().get(0);
                            log.info("I am the NEW coordinator: " + channel.getAddress());
                        }
                    }else{
                        log.info("Not the coordinator : "+ channel.getAddress());
                    }
                }

                public void receive(Message msg) {
                    
                    String request = new String(msg.getBuffer());
                    log.info("received msg from " + msg.getSrc() + ": " + request);
                    
                    if(StringUtils.contains("GiveMeYourHostnameAndPort",request)) {
                        try {
                            
                            String url = InetAddress.getLocalHost().getHostName()+':'+
                                    ServerFactory.getServer().findServices()[0].findConnectors()[0].getPort()+
                                    contextPath;
                            channel.send(msg.getSrc(),url.getBytes());
                            
                        } catch (Exception e) {
                            String srcIp="";   
                            Address addr = msg.getSrc();

                            PhysicalAddress physicalAddr = (PhysicalAddress)channel.down(new Event(Event.GET_PHYSICAL_ADDRESS, addr));

                            if(physicalAddr instanceof IpAddress) {
                                IpAddress ipAddr = (IpAddress)physicalAddr;
                                InetAddress inetAddr = ipAddr.getIpAddress();
                                srcIp = inetAddr.getHostAddress();
                            }
                            
                            log.info("Url information could not send back to {}",srcIp);
                        }
                        
                    }
                    
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
            throw new RuntimeException(e);
        }
        return channelToReturn;
    }

    @Override
    public void contextDestroyed(ServletContextEvent arg0) {
        if(channel != null) {
            channel.close();
        }
        
    }

    private static String contextPath="";
    
    //Run this before web application is started
    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        contextPath = servletContextEvent.getServletContext().getContextPath();
        System.out.println("HAController started "+contextPath); 
    }

}
