/*
 * Copyright 2022-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nctu.winlab.ProxyArp;

import org.onlab.packet.MacAddress;
import org.onosproject.cfg.ComponentConfigService;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.*;
// import org.onosproject.net.neighbour.DefaultNeighbourMessageHandler;
import org.onosproject.net.neighbour.NeighbourResolutionService;
import org.onosproject.net.neighbour.NeighbourMessageContext;
import org.onosproject.net.neighbour.NeighbourMessageHandler;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.edge.EdgePortEvent;
import org.onosproject.net.edge.EdgePortListener;
import org.onosproject.net.edge.EdgePortService;
import org.onosproject.net.host.HostService;
import static org.onosproject.net.HostId.hostId;

import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.Properties;
import java.util.Map;

import org.onlab.packet.IpAddress;

import static org.onlab.util.Tools.get;

/**
 * Skeletal ONOS application component.
 */
@Component(immediate = true,
           service = {SomeInterface.class},
           property = {
               "someProperty=Some Default String Value",
           })
public class AppComponent implements SomeInterface {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String APP_NAME = "nctu.winlab.ProxyArp";


    /** Some configurable property. */
    private String someProperty;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ComponentConfigService cfgService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected EdgePortService edgeService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected NeighbourResolutionService neighbourResolutionService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    private ApplicationId appId;

    protected Map<IpAddress, MacAddress> arp_tables;

    private InternalEdgeListener edgeListener = new InternalEdgeListener();
    // private DefaultNeighbourMessageHandler defaultHandler = new DefaultNeighbourMessageHandler();
    private ArpNeighbourMessageHandler defaultHandler = new ArpNeighbourMessageHandler();

    @Activate
    protected void activate() {
        appId = coreService.registerApplication(APP_NAME);
        cfgService.registerProperties(getClass());
        edgeService.addListener(edgeListener);
        edgeService.getEdgePoints().forEach(this::addDefault);
        arp_tables = Maps.newConcurrentMap();
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        cfgService.unregisterProperties(getClass(), false);
        edgeService.removeListener(edgeListener);
        neighbourResolutionService.unregisterNeighbourHandlers(appId);
        log.info("Stopped");
    }

    private void addDefault(ConnectPoint port) {
        neighbourResolutionService.registerNeighbourHandler(port, defaultHandler, appId);
    }

    private void removeDefault(ConnectPoint port) {
        neighbourResolutionService.unregisterNeighbourHandler(port, defaultHandler, appId);
    }

    private class InternalEdgeListener implements EdgePortListener {
        @Override
        public void event(EdgePortEvent event) {
            switch (event.type()) {
            case EDGE_PORT_ADDED:
                addDefault(event.subject());
                log.info("addDefault:", event.subject().toString());
                break;
            case EDGE_PORT_REMOVED:
                removeDefault(event.subject());
                log.info("removeDefault:", event.subject().toString());
                break;
            default:
                break;
            }
        }
    }
    public class ArpNeighbourMessageHandler implements NeighbourMessageHandler{
        // protected Map<IpAddress, MacAddress> arp_tables;
        // private final Logger log = LoggerFactory.getLogger(getClass());
        @Override
        public void handleMessage(NeighbourMessageContext context, HostService hostService) {
            IpAddress ip = context.sender();
            MacAddress mac = context.srcMac();
            arp_tables.putIfAbsent(ip, mac);
            switch (context.type()) {
                case REQUEST:
                    if (arp_tables.get(context.target()) == null) {
                        context.flood();
                        log.info("TABLE MISS. Send request to edge ports.");
                    } 
                    else {
                        context.reply(arp_tables.get(context.target()));
                        log.info("TABLE HIT. Requested MAC = {}", arp_tables.get(context.target()));
                    }
                    break;
                case REPLY:
                    Host h = hostService.getHost(hostId(context.dstMac()));
                    // context.forward(Interface(context.dstMac()));
                    context.forward(h.location());
                    log.info("RECV REPLY. Requested MAC = {}", context.dstMac());
                    break;
                default:
                    break;
            }
            
        }
        private ConnectPoint Interface(MacAddress dstMac) {
            return null;
        }
        
    }
    
    

    @Modified
    public void modified(ComponentContext context) {
        Dictionary<?, ?> properties = context != null ? context.getProperties() : new Properties();
        if (context != null) {
            someProperty = get(properties, "someProperty");
        }
        log.info("Reconfigured");
    }

    @Override
    public void someMethod() {
        log.info("Invoked");
    }

}

