package com.acneuro.govner.jgroups;

import java.util.Set;

public abstract class Observer {
	
   protected HAController haController;
   
   public abstract void notifyNodesJoin(Set<Node> node);
   
   public abstract void notifyNodesLeave(Set<Node> node);
   
}