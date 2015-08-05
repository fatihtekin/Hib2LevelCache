package com.acneuro.govner.jgroups;

public class Node {

	private String ip;
	private String hostName;
	private String nodeId;
	private String contextPath;	
	
	private Node() {}

	public static class Builder {

		private String ip;
		private String hostName;
		private String nodeId;
		private String contextPath;
		
        public Builder ip(final String ip) {
            this.ip = ip;
            return this;
        }

        public Builder hostName(final String hostName) {
            this.hostName = hostName;
            return this;
        }


		public Builder nodeId(String nodeId) {
			this.nodeId = nodeId;
            return this;
		}

		public Builder contextPath(String contextPath) {
			this.contextPath = contextPath;
            return this;
		}

		public Node build() {
        	Node node = new Node();
        	node.contextPath = this.contextPath;
        	node.ip = this.ip;
        	node.hostName = this.hostName;
        	node.nodeId = this.nodeId;
            return node;
        }
    }

    public static Builder builder() {
        return new Builder();
    }

	public String getIp() {
		return ip;
	}

	public String getHostName() {
		return hostName;
	}

	public String getNodeId() {
		return nodeId;
	}

	public String getContextPath() {
		return contextPath;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((hostName == null) ? 0 : hostName.hashCode());
		result = prime * result + ((ip == null) ? 0 : ip.hashCode());
		result = prime * result + ((nodeId == null) ? 0 : nodeId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Node other = (Node) obj;
		if (hostName == null) {
			if (other.hostName != null)
				return false;
		} else if (!hostName.equals(other.hostName))
			return false;
		if (ip == null) {
			if (other.ip != null)
				return false;
		} else if (!ip.equals(other.ip))
			return false;
		if (nodeId == null) {
			if (other.nodeId != null)
				return false;
		} else if (!nodeId.equals(other.nodeId))
			return false;
		return true;
	}

	
    
    
	
}
