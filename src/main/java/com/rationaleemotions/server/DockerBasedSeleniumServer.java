package com.rationaleemotions.server;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.openqa.grid.internal.TestSession;
import org.openqa.selenium.remote.CapabilityType;

import com.rationaleemotions.config.BrowserVersionInfo;
import com.rationaleemotions.config.ConfigReader;
import com.rationaleemotions.server.docker.ContainerAttributes;
import com.spotify.docker.client.UnixConnectionSocketFactory;
import com.spotify.docker.client.exceptions.DockerException;

/**
 * Represents a {@link ISeleniumServer} implementation that is backed by a docker container which executes the
 * selenium server within a docker container.
 *
 */
public class DockerBasedSeleniumServer implements ISeleniumServer {
	protected DockerHelper.ContainerInfo containerInfo;

    @Override
    public int startServer(TestSession session) throws ServerException {
        try {
        	containerInfo = DockerHelper.startContainerFor(getContainerAttributes(session.getRequestedCapabilities()));
            session.put("container", containerInfo);
            return containerInfo.getPort();
        } catch (DockerException | InterruptedException e) {
            throw new ServerException(e.getMessage(), e);
        }
    }

    @Override
    public int getPort() {
        return this.containerInfo.getPort();
    }

    @Override
	public String getHost() {
    	URI uri=ConfigReader.getInstance().getDockerRestApiUri();
		if (uri.getScheme().equals(DockerHelper.UNIX_SCHEME)) {
			return UnixConnectionSocketFactory.sanitizeUri(uri).getHost();
		} else {
			return uri.getHost();
		}
	}

    @Override
    public void shutdownServer() throws ServerException {
        try {
            DockerHelper.killAndRemoveContainer(containerInfo.getContainerId());
        } catch (DockerException | InterruptedException e) {
            throw new ServerException(e.getMessage(), e);
        }
    }
    
    private ContainerAttributes getContainerAttributes(Map<String, Object> requestedCapabilities) {
    	String browser = (String) requestedCapabilities.get(CapabilityType.BROWSER_NAME);
    	String version = (String) requestedCapabilities.get(CapabilityType.BROWSER_VERSION);
    	
    	BrowserVersionInfo browserVersion = ConfigReader.getInstance().getBrowserVersion(browser, version);
        String image = browserVersion.getTargetAttribute("image").toString();
        int port = Integer.parseInt(browserVersion.getTargetAttribute("port").toString());
        
        List<String> ports = browserVersion.getTargetAttributeAsList("ports");
        List<String> volumes = browserVersion.getTargetAttributeAsList("volumes");
        
        List<String> envs=new ArrayList<>();
        List<String> configEnv = browserVersion.getTargetAttributeAsList("env");
        List<String> capabilitiesEnv = getConfiguredDockerEnvsFromCapabilities(requestedCapabilities);
        if(configEnv!=null && !configEnv.isEmpty()){
        	envs.addAll(configEnv);
        }
        if(capabilitiesEnv!=null && !capabilitiesEnv.isEmpty()){
        	envs.addAll(capabilitiesEnv);
        }
        
        Long shmSize=getShmSize(browserVersion.getTargetAttribute("shmSize"));
        
    	ContainerAttributes containerAttributes = new ContainerAttributes();
    	containerAttributes.setImage(image);
    	containerAttributes.setPort(port);
    	containerAttributes.setPorts(ports);
    	containerAttributes.setVolumes(volumes);
    	containerAttributes.setEnvs(envs);
    	containerAttributes.setPrivileged(false);
    	containerAttributes.setShmSize(shmSize);
		return containerAttributes;
    }

	private List<String> getConfiguredDockerEnvsFromCapabilities(final Map<String, Object> requestedCapabilities) {
	    
		List<String> envs = new ArrayList<>();
		for(Entry<String, Object> capability:requestedCapabilities.entrySet()){
			if(capability.getKey().startsWith("DOCKER_ENV_")){
				String value=capability.getValue()!=null?capability.getValue().toString():null;
				envs.add(capability.getKey().replace("DOCKER_ENV_", "")+"="+value);
			}
		}
		return envs;
	}

    private Long getShmSize(Object shmSize) {
    	if(shmSize==null){
    		return null;
    	}
    	try{
    		if(shmSize.toString().endsWith("b")){
	    		return Long.parseLong(shmSize.toString().replace("b", ""));
	    		
	    	} else if(shmSize.toString().endsWith("k")){
	    		return Long.parseLong(shmSize.toString().replace("k", ""))*1024L;
	    		
	    	} else if(shmSize.toString().endsWith("m")){
	    		return Long.parseLong(shmSize.toString().replace("m", ""))*1024L*1024L*1024L;
	    		
	    	} else if(shmSize.toString().endsWith("g")){
	    		return Long.parseLong(shmSize.toString().replace("g", ""))*1024L*1024L*1024L*1024L;
	    		
	    	} else {
	    		return Long.parseLong(shmSize.toString())*1024L*1024L*1024L*1024L;
	    	}
    	}catch (NumberFormatException e) {
			e.printStackTrace();
		}
    	return null;
    }
}
