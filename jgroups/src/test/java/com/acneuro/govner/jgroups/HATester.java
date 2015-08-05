package com.acneuro.govner.jgroups;

import java.util.Set;

public class HATester extends Observer{

	
	public static void main(String[] args) throws Exception {
		
		HAController controller = HAController.getInstance();
		HATester haTester = new HATester();
		controller.attach(haTester);
		
		Thread.sleep(60000);
		
		
	}

	@Override
	public void notifyNodesJoin(Set<Node> node) {
		System.out.println(node.size());
	}

	@Override
	public void notifyNodesLeave(Set<Node> node) {
		System.out.println(node.size());			
		System.out.println(HAController.getNodes().size());
	}
	
}
