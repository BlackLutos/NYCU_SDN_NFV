/*
 * Copyright 2020-present Open Networking Foundation
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
package nctu.winlab.bridge;

import com.google.common.collect.ImmutableSet;
import com.sun.tools.jdi.Packet;
import org.onosproject.cfg.ComponentConfigService;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.Properties;

import static org.onlab.util.Tools.get;

/**
 * My new import class.
 */
import java.util.Map;
import com.google.common.collect.Maps;
import org.onlab.packet.VlanId;
import org.onlab.packet.Ethernet;
import org.onlab.packet.MacAddress;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.*;
import org.onosproject.net.host.HostService;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketService;
import org.onosproject.net.packet.PacketPriority;
import org.onosproject.net.flow.*;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.flowobjective.ForwardingObjective;


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

    /** Some configurable property. */
    private String someProperty;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ComponentConfigService cfgService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected FlowRuleService flowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected FlowObjectiveService flowObjectiveService;

    private static final int FLOW_TIMEOUT = 30;

    private static final int FLOW_PRIORITY = 30;

    protected Map<DeviceId, Map<MacAddress, PortNumber>> tables;

    private ApplicationId appId;
    private MyReativePacketProcessor processor;

    @Activate
    protected void activate() {
        cfgService.registerProperties(getClass());
        log.info("Started");

        appId = coreService.registerApplication("nctu.winlab.bridge");
        processor = new MyReativePacketProcessor();
        packetService.addProcessor(processor, PacketProcessor.director(2));
        tables = Maps.newConcurrentMap();

        requestIntercepts();
    }

    @Deactivate
    protected void deactivate() {
        cfgService.unregisterProperties(getClass(), false);
        log.info("Stopped");

        packetService.removeProcessor(processor);
        flowRuleService.removeFlowRulesById((appId));
        processor = null;

        withdrawIntercepts();
    }

    @Modified
    public void modified(ComponentContext context) {
        Dictionary<?, ?> properties = context != null ? context.getProperties() : new Properties();
        if (context != null) {
            someProperty = get(properties, "someProperty");
        }
        log.info("Reconfigured");

        tables = Maps.newConcurrentMap();

        requestIntercepts();
    }

    @Override
    public void someMethod() {
        log.info("Invoked");
    }

    /**
     * Request packet in via packet service.
     */
    private void requestIntercepts() {
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.matchEthType(Ethernet.TYPE_IPV4);
        packetService.requestPackets(selector.build(), PacketPriority.REACTIVE, appId);
    }

    /**
     * Cancel request for packet in via packet service.
     */
    private void withdrawIntercepts() {
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.matchEthType(Ethernet.TYPE_IPV4);
        packetService.cancelPackets(selector.build(), PacketPriority.REACTIVE, appId);
    }

    /**
     * My packet processor responsible for forwarding packets along their paths.
     */
    private class MyReativePacketProcessor implements PacketProcessor {
        @Override
        public void process(PacketContext context) {
            //Stop processing if the packet has been handled, since we can't do any more to it.
            if (context.isHandled()) {
                return;
            }

            InboundPacket pkt = context.inPacket();
            Ethernet ethPkt = pkt.parsed();
            ConnectPoint cp = pkt.receivedFrom();

            if (ethPkt == null) {
                return;
            }

            //Update the MAC address table (source MAC address <=> incoming port) when the controller receives Packet-in.
            tables.putIfAbsent(cp.deviceId(), Maps.newConcurrentMap());
            tables.get(cp.deviceId()).putIfAbsent(ethPkt.getSourceMAC(), cp.port());
            log.info("Add an entry to the port table of " + cp.deviceId() + "MAC address: " + ethPkt.getSourceMAC() + " => Port: " + cp.port());

            //Check if the destination MAC address is in the MAC address table.
            //table miss
            if (tables.get(cp.deviceId()).get(ethPkt.getDestinationMAC()) == null) {
                //Send Packet-out to every port on the switch which send Packet-in, except for th Packet-in port.
                Ethernet Pkt = context.inPacket().parsed();
                DeviceId deviceId= context.inPacket().receivedFrom().deviceId();
                packetOut(context, PortNumber.FLOOD);
                log.info("MAC address " + Pkt.getDestinationMAC() + "is missed on " + deviceId + ". Flood the packet.");
                return;
            }
            //table hit
            else {
                //Install flow rule on the switch which send Packet-in.
                installRule(context, cp.port(), tables.get(cp.deviceId()).get(ethPkt.getDestinationMAC()));
                //Send Packet-out to the  port(related to the destination MAC address) on the switch which send Packet-in.
                packetOut(context, tables.get(cp.deviceId()).get(ethPkt.getDestinationMAC()));
                return;
            }
        }
    }

    /**
     * Sends a packet out the specified port.
     * @param context
     * @param portNumber
     */
    private void packetOut(PacketContext context, PortNumber portNumber) {
        context.treatmentBuilder().setOutput((portNumber));
        context.send();
    }

    /**
     * Install a rule forwarding the packet to the specified port.
     * @param context
     * @param inputPortNumber
     * @param outputPortNumber
     */
    private void installRule(PacketContext context, PortNumber inputPortNumber, PortNumber outputPortNumber) {
        Ethernet ethPkt = context.inPacket().parsed();
        DeviceId deviceId= context.inPacket().receivedFrom().deviceId();
        TrafficSelector selector;
        TrafficTreatment treatment;
        ForwardingObjective forwardingObjective;

        selector = DefaultTrafficSelector.builder()
                .matchEthSrc(ethPkt.getSourceMAC())
                .matchEthDst(ethPkt.getDestinationMAC())
                .build();
        treatment = DefaultTrafficTreatment.builder()
                .setOutput(outputPortNumber)
                .build();
        forwardingObjective = DefaultForwardingObjective.builder()
                .withSelector(selector)
                .withTreatment(treatment)
                .withPriority(FLOW_PRIORITY)
                .withFlag(ForwardingObjective.Flag.VERSATILE)
                .makeTemporary(FLOW_TIMEOUT)
                .fromApp(appId)
                .add();
        flowObjectiveService.forward(deviceId, forwardingObjective);

        selector = DefaultTrafficSelector.builder()
                .matchEthSrc(ethPkt.getDestinationMAC())
                .matchEthDst(ethPkt.getSourceMAC())
                .build();
        treatment = DefaultTrafficTreatment.builder()
                .setOutput(inputPortNumber)
                .build();
        forwardingObjective = DefaultForwardingObjective.builder()
                .withSelector(selector)
                .withTreatment(treatment)
                .withPriority(FLOW_PRIORITY)
                .withFlag(ForwardingObjective.Flag.VERSATILE)
                .makeTemporary(FLOW_TIMEOUT)
                .fromApp(appId)
                .add();

        flowObjectiveService.forward(deviceId, forwardingObjective);

        log.info("MAC address " + ethPkt.getDestinationMAC() + "is matched on " + deviceId + ". Install a flow rule.");
    }
}
