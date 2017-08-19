package com.rationaleemotions.server;

import static com.rationaleemotions.config.ConfigReader.getInstance;
import static com.spotify.docker.client.DockerClient.LogsParam.stderr;
import static com.spotify.docker.client.DockerClient.LogsParam.stdout;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.openqa.selenium.net.PortProber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.rationaleemotions.config.BrowserInfo;
import com.rationaleemotions.config.BrowserVersionInfo;
import com.rationaleemotions.server.ISeleniumServer.ServerException;
import com.rationaleemotions.server.docker.ContainerAttributes;
import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.LogStream;
import com.spotify.docker.client.LoggingBuildHandler;
import com.spotify.docker.client.ProgressHandler;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerCreation;
import com.spotify.docker.client.messages.HostConfig;
import com.spotify.docker.client.messages.Image;
import com.spotify.docker.client.messages.PortBinding;

/**
 * A Helper class that facilitates interaction with a Docker Daemon.
 */
class DockerHelper {
    private final static Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    
    public static final String UNIX_SCHEME = "unix";
    
    private DockerHelper() {
        DockerClient client = getClient();
        Runtime.getRuntime().addShutdownHook(new Thread(new DockerCleanup(client)));
    }

    /**
     * @param id - The ID of the container that is to be cleaned up.
     * @throws DockerException      - In case of any issues.
     * @throws InterruptedException - In case of any issues.
     */
    static void killAndRemoveContainer(final String id) throws DockerException, InterruptedException {
        LOG.debug("Killing and removing the container : [{}].", id);
        
        try {
			getClient().killContainer(id);
		} catch (DockerException | InterruptedException e) {
			// Fail if containers already stopped
			LOG.error("Fail to kill container {}", id, e);
		}
        getClient().removeContainer(id);
    }

    /**
     * @param id - The ID of the container that is to be removed.
     * @throws DockerException      - In case of any issues.
     * @throws InterruptedException - In case of any issues.
     */
    static void removeContainer(final String id) throws DockerException, InterruptedException {
    	LOG.debug("Removing the container : [{}].", id);
        getClient().removeContainer(id);
    }

    /**
     * @param image - The name of the image for which a docker container is to be spun off. For e.g., you could
     *              specify the image name as <code>selenium/standalone-chrome:3.0.1</code> to download the
     *              <code>standalone-chrome</code> image with its tag as <code>3.0.1</code>
     * @param isPrivileged - <code>true</code> if the container is to be run in privileged mode.
     * @param devices - A List of {@link DeviceInfo} objects
     * @return - A {@link ContainerInfo} object that represents the newly spun off container.
     * @throws DockerException      - In case of any issues.
     * @throws InterruptedException - In case of any issues.
     * @throws ServerException 
     */
    static ContainerInfo startContainerFor(final ContainerAttributes containerAttributes) throws DockerException, InterruptedException, ServerException {
    	LOG.debug("Starting of container for the image [{}].", containerAttributes.getImage());

        Preconditions.checkState("ok".equalsIgnoreCase(getClient().ping()),
            "Ensuring that the Docker Daemon is up and running.");
        DockerHelper.predownloadImagesIfRequired();

        final Map<String, List<PortBinding>> portBindings = new HashMap<>();

        String localHost = getInstance().getLocalhost();
        List<String> exposedPorts=new LinkedList<>();
        if(containerAttributes.getPorts()!=null){
        	exposedPorts=new LinkedList<>(containerAttributes.getPorts());
        }
        exposedPorts.add(Integer.toString(containerAttributes.getPort()));
        int containerPort = 0;
        for (String port : exposedPorts) {
            int randomPort = PortProber.findFreePort();
            if(Integer.parseInt(port)==containerAttributes.getPort()){
            	containerPort=randomPort;
            }
            PortBinding binding = PortBinding.create(localHost, Integer.toString(randomPort));
            List<PortBinding> portBinding = new ArrayList<>();
            portBinding.add(binding);
            portBindings.put(port, portBinding);
        }
        final HostConfig hostConfig = HostConfig.builder()
        		.portBindings(portBindings)
        		.privileged(containerAttributes.isPrivileged())
        		.binds(containerAttributes.getVolumes())
        		.autoRemove(true)
        		.shmSize(containerAttributes.getShmSize())
        		.build();

        final ContainerConfig containerConfig = ContainerConfig.builder()
            .hostConfig(hostConfig)
            .image(containerAttributes.getImage())
            .exposedPorts(new HashSet<String>(exposedPorts))
            .env(containerAttributes.getEnvs())
            .build();
        final ContainerCreation creation = getClient().createContainer(containerConfig);
        final String id = creation.id();

        com.spotify.docker.client.messages.ContainerInfo containerInfo = getClient().inspectContainer(id);
        if (! containerInfo.state().running()) {
            // Start container
            getClient().startContainer(id);
            LOG.debug("Starting {}", containerInfo.name());
        } else {
        	LOG.debug("{} was already running.", containerInfo.name());
        }
        
        waitContainerAvailable(id);

        ContainerInfo info = new ContainerInfo(id, containerPort);
        LOG.debug("******{}******", info);

        return info;
    }

	/**
	 * @param id
	 * @throws InterruptedException
	 * @throws DockerException
	 * @throws ServerException
	 */
	private static void waitContainerAvailable(final String id)
			throws InterruptedException, DockerException, ServerException {
		com.spotify.docker.client.messages.ContainerInfo containerInfo;
		AtomicInteger attempts = new AtomicInteger(0);
        Integer exitCode=0;
        do{
            // Inspect container
        	TimeUnit.SECONDS.sleep(1);
            containerInfo = getClient().inspectContainer(id);
            exitCode=containerInfo.state().exitCode();
            if(exitCode==null){
            	exitCode=0;
            }
            LOG.debug("Container {} with id {} - Information {}", containerInfo.name(), id, containerInfo);

        }while(!containerInfo.state().running() && exitCode==0 && attempts.incrementAndGet()<=10);
        
        if(!containerInfo.state().running() || exitCode!=0){
        	try (LogStream stream = getClient().logs(id, stdout(), stderr())) {
        		final String logs;
                logs = stream.readFully();
                LOG.error("Container logs:\n{}", logs);
            	
            } catch (Exception e) {
            	LOG.error("Fail to retrieve container logs...",e);
			}
        	LOG.error("Failed to start Container {} with id {} - Information {}", containerInfo.name(), id, containerInfo);
        	
        	if(exitCode!=0){
        		//Fail to start container, remove it!
        		removeContainer(id);
        	}else{
        		//No exit code? System busy? try to kill it and remove it!
        		killAndRemoveContainer(id);
        	}
        	
            throw new ServerException(String.format("Failed to start Container %s with id %s", containerInfo.name(), id));
        }
	}

    private static void predownloadImagesIfRequired() throws DockerException, InterruptedException {

        DockerClient client = getClient();
        LOG.info("Start downloading of images.");
        Collection<BrowserInfo> browsers = getInstance().getBrowsers().values();

        Set<String> dockerImages = new HashSet<>();
        for (BrowserInfo browser : browsers) {
        	for (BrowserVersionInfo browserVersion : browser.getVersions()) {
	        	String dockerImage=browserVersion.getTargetAttribute("image").toString();
	            
	        	dockerImages.add(dockerImage);
	        	
        	}
        }
        ProgressHandler handler = new LoggingBuildHandler();
        for (Iterator<String> iterator = dockerImages.iterator(); iterator.hasNext();) {
			String dockerImage = iterator.next();
        	List<Image> foundImages = client.listImages(DockerClient.ListImagesParam.byName(dockerImage));
            if (! foundImages.isEmpty()) {
                LOG.info(String.format("Skipping download for Image [%s] because it's already available.",
                		dockerImage));
                continue;
            }
            client.pull(dockerImage, handler);
		}
    }

    private static DockerClient getClient() {
        return DefaultDockerClient.builder().uri(getInstance().getDockerRestApiUri()).build();
    }

    /**
     * A Simple POJO that represents the newly spun off container, encapsulating the container Id and the port on which
     * the container is running.
     */
    public static class ContainerInfo {
        private int port;
        private String containerId;

        ContainerInfo(final String containerId, final int port) {
            this.port = port;
            this.containerId = containerId;
        }

        public int getPort() {
            return port;
        }

        public String getContainerId() {
            return containerId;
        }

        @Override
        public String toString() {
            return String.format("%s running on %d", containerId, port);
        }
    }


    private static class DockerCleanup implements Runnable {
        private DockerClient client;

        DockerCleanup(final DockerClient client) {
            this.client = client;
        }

        @Override
        public void run() {
            if (client != null) {
                client.close();
            }
        }
    }

}
