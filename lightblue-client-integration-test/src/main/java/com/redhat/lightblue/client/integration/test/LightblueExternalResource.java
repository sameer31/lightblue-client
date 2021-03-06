package com.redhat.lightblue.client.integration.test;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;

import org.junit.runners.model.TestClass;

import com.fasterxml.jackson.databind.JsonNode;
import com.redhat.lightblue.client.LightblueClient;
import com.redhat.lightblue.client.LightblueException;
import com.redhat.lightblue.client.response.LightblueResponse;
import com.redhat.lightblue.rest.integration.LightblueRestTestHarness;

import io.undertow.security.idm.IdentityManager;

public class LightblueExternalResource extends BeforeAfterTestRule {

    private final static int DEFAULT_PORT = 8000;

    @Deprecated
    public interface LightblueTestMethods extends LightblueTestHarnessConfig {
    }

    public interface LightblueTestHarnessConfig {
        JsonNode[] getMetadataJsonNodes() throws Exception;

        default boolean isGrantAnyoneAccess() {
            return true;
        }

    }

    private final LightblueTestHarnessConfig methods;
    private final int httpServerPort;
    private IdentityManager identityManager;
    private boolean removeHooks = Boolean.TRUE;

    private ArtificialLightblueClientCRUDController controller;

    public LightblueExternalResource(LightblueTestHarnessConfig methods) {
        this(methods, DEFAULT_PORT);
    }

    public LightblueExternalResource(LightblueTestHarnessConfig methods, IdentityManager identityManager) {
        this(methods, DEFAULT_PORT);
    }

    public LightblueExternalResource(LightblueTestHarnessConfig methods, boolean removeHooks) {
        this(methods, removeHooks, null);
    }

    public LightblueExternalResource(LightblueTestHarnessConfig methods, boolean removeHooks, IdentityManager identityManager) {
        this(methods, DEFAULT_PORT, identityManager);
        this.removeHooks = removeHooks;
    }

    public LightblueExternalResource(LightblueTestHarnessConfig methods, Integer httpServerPort) {
        this(methods, httpServerPort, null);
    }

    public LightblueExternalResource(LightblueTestHarnessConfig methods, Integer httpServerPort, IdentityManager identityManager) {
        super(new TestClass(ArtificialLightblueClientCRUDController.class));

        if (methods == null) {
            throw new IllegalArgumentException("Must provide an instance of LightblueTestMethods");
        }
        this.methods = methods;
        this.httpServerPort = httpServerPort;
        this.identityManager = identityManager;
    }

    protected LightblueClientTestHarness getControllerInstance() {
        if (controller == null) {
            try {
                if (removeHooks) {
                    controller = new ArtificialLightblueClientCRUDController(httpServerPort, identityManager);
                } else {
                    controller = new ArtificialLightblueClientCRUDControllerWithHooks(httpServerPort, identityManager);
                }
            } catch (Exception e) {
                throw new RuntimeException("Unable to create test CRUD Controller", e);
            }
        }
        return controller;
    }

    public LightblueClient getLightblueClient() {
        return getControllerInstance().getLightblueClient();
    }

    public LightblueClient getLightblueClient(String username, String password) {
        LightblueClientTestHarness harness = getControllerInstance();
        return harness.getLightblueClient(harness.getLightblueClientConfiguration(username, password));
    }

    public LightblueResponse loadData(String entityName, String entityVersion, String resourcePath) throws IOException, LightblueException {
        return getControllerInstance().loadData(entityName, entityVersion,
                resourcePath);
    }

    public Set<String> getEntityVersions(String entityName) throws LightblueException {
        return getControllerInstance().getEntityVersions(entityName);
    }

    public void cleanupMongoCollections(String... collectionNames) throws UnknownHostException {
        getControllerInstance().cleanupMongoCollections(collectionNames);
    }

    public void cleanupMongoCollections(String dbName, String[] collectionNames) throws UnknownHostException {
        getControllerInstance().cleanupMongoCollections(dbName, collectionNames);
    }

    public void ensureHttpServerIsRunning() throws IOException {
        getControllerInstance().ensureHttpServerIsRunning();
    }

    public int getHttpPort() {
        return getControllerInstance().getHttpPort();
    }

    public String getDataUrl() {
        return getControllerInstance().getDataUrl();
    }

    public String getMetadataUrl() {
        return getControllerInstance().getMetadataUrl();
    }

    /**
     * <p>This method will change the {@link IdentityManager} that the server is using. Be warned, this method will
     * internal restart lightblue.</p>
     * <p>A <code>null</code> value is the same as no authentication.
     * @param identityManager - implementation of {@link IdentityManager}.
     * @throws IOException
     */
    public void changeIdentityManager(IdentityManager identifyManager) throws IOException {
        identityManager = identifyManager;
        getControllerInstance().setIdentityManager(identifyManager);
        LightblueRestTestHarness.stopHttpServer();
        ensureHttpServerIsRunning();

        //TODO remove sleep. There is some sort of timing issue with the server restart.
        //DeploymentManager might be the key, but no way to get access to it today
        // https://github.com/undertow-io/undertow/blob/master/examples/src/main/java/io/undertow/examples/servlet/ServletServer.java
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private class ArtificialLightblueClientCRUDController extends LightblueClientTestHarness {

        public ArtificialLightblueClientCRUDController(int httpServerPort, IdentityManager identityManager) throws Exception {
            super(httpServerPort, identityManager);
        }

        @Override
        protected JsonNode[] getMetadataJsonNodes() throws Exception {
            return methods.getMetadataJsonNodes();
        }

        @Override
        public boolean isGrantAnyoneAccess() {
            return methods.isGrantAnyoneAccess();
        }

    }

    private class ArtificialLightblueClientCRUDControllerWithHooks extends ArtificialLightblueClientCRUDController {

        public ArtificialLightblueClientCRUDControllerWithHooks(int httpServerPort, IdentityManager identityManager) throws Exception {
            super(httpServerPort, identityManager);
        }

        @Override
        public Set<String> getHooksToRemove() {
            return new HashSet<>();
        }

    }

}
