package com.rationaleemotions.server.docker;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class ContainerAttributes {
	private String image;
	private int port;
    private boolean isPrivileged;
    private List<String> ports;
    private List<String> volumes;
    private List<String> envs;
	private Long shmSize;

	public String getImage() {
        return this.image;
    }

	public void setImage(final String image) {
		this.image = image;
	}

    public boolean isPrivileged() {
        return this.isPrivileged;
    }

    public void setPrivileged(final boolean privileged) {
        isPrivileged = privileged;
    }

	public List<String> getVolumes() {
		if(this.volumes==null){
			return Collections.emptyList();
		}
		return Collections.unmodifiableList(new LinkedList<String>(this.volumes));
	}

	public void setVolumes(final List<String> volumes) {
		this.volumes = volumes;
	}

    public void setVolumes(final String... volumes) {
        this.volumes = Arrays.asList(volumes);
    }

	public List<String> getPorts() {
		if(this.ports==null){
			return Collections.emptyList();
		}
		return Collections.unmodifiableList(new LinkedList<String>(this.ports));
	}
	
	public void setPorts(final List<String> ports) {
		this.ports = ports;
	}
	
	public void setPorts(final String... ports) {
		this.ports = Arrays.asList(ports);
	}

	public List<String> getEnvs() {
		if(this.envs==null){
			return Collections.emptyList();
		}
		return Collections.unmodifiableList(new LinkedList<String>(this.envs));
	}

	public void setEnvs(final List<String> envs) {
		this.envs = envs;
	}
	
	public void setEnvs(final String... envs) {
		this.envs = Arrays.asList(envs);
	}

	public int getPort() {
		return port;
	}

	public void setPort(final int port) {
		this.port = port;
	}

    public Long getShmSize() {
		return shmSize;
	}

	public void setShmSize(final Long shmSize) {
		this.shmSize = shmSize;
	}
}
