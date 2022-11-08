//CHECKSTYLE:OFF
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
package nctu.winlab.unicastdhcp;

import static org.onosproject.net.config.NetworkConfigEvent.Type.CONFIG_ADDED;
import static org.onosproject.net.config.NetworkConfigEvent.Type.CONFIG_UPDATED;
import static org.onosproject.net.config.basics.SubjectFactories.APP_SUBJECT_FACTORY;

import javax.security.auth.kerberos.KerberosCredMessage;

import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.onlab.packet.Ethernet;
// import org.onosproject.net.Host;
// import org.onosproject.net.HostId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
// import org.onosproject.net.PortNumber;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
// import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.host.HostService;
// import org.onosproject.net.intent.HostToHostIntent;
import org.onosproject.net.intent.IntentService;
// import org.onosproject.net.intent.IntentState;
import org.onosproject.net.FilteredConnectPoint;
import org.onosproject.net.intent.Key;
import org.onosproject.net.intent.PointToPointIntent;
import org.onosproject.net.packet.DefaultOutboundPacket;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.OutboundPacket;
// import org.onosproject.net.packet.PortNumber;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketPriority;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.onlab.packet.IPv4;
import org.onlab.packet.TpPort;
import org.onlab.packet.UDP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;





/** Sample Network Configuration Service Application. **/
@Component(immediate = true)
public class AppComponent {

  private final Logger log = LoggerFactory.getLogger(getClass());
  private final NameConfigListener cfgListener = new NameConfigListener();
  private final ConfigFactory<ApplicationId, DhcpConfig> factory = new ConfigFactory<ApplicationId, DhcpConfig>(
      APP_SUBJECT_FACTORY, DhcpConfig.class, "UnicastDhcpConfig") {
    @Override
    public DhcpConfig createConfig() {
      return new DhcpConfig();
    }
  };

  private ApplicationId appId;

  @Reference(cardinality = ReferenceCardinality.MANDATORY)
  protected NetworkConfigRegistry cfgService;

  @Reference(cardinality = ReferenceCardinality.MANDATORY)
  protected IntentService intentService;

  @Reference(cardinality = ReferenceCardinality.MANDATORY)
  protected CoreService coreService;

  @Reference(cardinality = ReferenceCardinality.MANDATORY)
  protected PacketService packetService;

  // private static final int FLOW_TIMEOUT = 30;

  private static final int PRIORITY = 30;

  private ConnectPoint dhcpServerCp;
  private HostService hostService;
  private DhcpPacketProcessor processor = new DhcpPacketProcessor();



  @Activate
  protected void activate() {
    appId = coreService.registerApplication("nctu.winlab.unicastdhcp");
    processor = new DhcpPacketProcessor();
    packetService.addProcessor(processor, PacketProcessor.director(2));
    cfgService.addListener(cfgListener);
    cfgService.registerConfigFactory(factory);
    log.info("Started Unicastdhcp");

    requestDhcpPackets();
  }

  @Deactivate
  protected void deactivate() {
    packetService.removeProcessor(processor);
    cfgService.removeListener(cfgListener);
    cfgService.unregisterConfigFactory(factory);
    log.info("Stopped");

    cancelDhcpPackets();
  }
  private void requestDhcpPackets() {
    TrafficSelector.Builder selectorClient = DefaultTrafficSelector.builder()
            .matchEthType(Ethernet.TYPE_IPV4)
            .matchIPProtocol(IPv4.PROTOCOL_UDP)
            .matchUdpSrc(TpPort.tpPort(UDP.DHCP_CLIENT_PORT))
            .matchUdpDst(TpPort.tpPort(UDP.DHCP_SERVER_PORT));
    TrafficSelector.Builder selectorServer = DefaultTrafficSelector.builder()
            .matchEthType(Ethernet.TYPE_IPV4)
            .matchIPProtocol(IPv4.PROTOCOL_UDP)
            .matchUdpSrc(TpPort.tpPort(UDP.DHCP_SERVER_PORT))
            .matchUdpDst(TpPort.tpPort(UDP.DHCP_CLIENT_PORT));
    packetService.requestPackets(selectorClient.build(), PacketPriority.CONTROL, appId);
    packetService.requestPackets(selectorServer.build(), PacketPriority.CONTROL, appId);
  }
  private void cancelDhcpPackets() {
    TrafficSelector.Builder selectorClient = DefaultTrafficSelector.builder()
            .matchEthType(Ethernet.TYPE_IPV4)
            .matchIPProtocol(IPv4.PROTOCOL_UDP)
            .matchUdpSrc(TpPort.tpPort(UDP.DHCP_CLIENT_PORT))
            .matchUdpDst(TpPort.tpPort(UDP.DHCP_SERVER_PORT));
    TrafficSelector.Builder selectorServer = DefaultTrafficSelector.builder()
            .matchEthType(Ethernet.TYPE_IPV4)
            .matchIPProtocol(IPv4.PROTOCOL_UDP)
            .matchUdpSrc(TpPort.tpPort(UDP.DHCP_SERVER_PORT))
            .matchUdpDst(TpPort.tpPort(UDP.DHCP_CLIENT_PORT));
    packetService.cancelPackets(selectorClient.build(), PacketPriority.CONTROL, appId);
    packetService.cancelPackets(selectorServer.build(), PacketPriority.CONTROL, appId);
  }

  private class NameConfigListener implements NetworkConfigListener {
    @Override
    public void event(NetworkConfigEvent event) {
      if ((event.type() == CONFIG_ADDED || event.type() == CONFIG_UPDATED)
          && event.configClass().equals(DhcpConfig.class)) {
        DhcpConfig config = cfgService.getConfig(appId, DhcpConfig.class);
        if (config != null) {
          dhcpServerCp = ConnectPoint.deviceConnectPoint(config.name());
          log.info("Server is on {}!", config.name());
          log.info("DHCP server is connected to {}, port {}", dhcpServerCp.deviceId(), dhcpServerCp.port());
        }
      }
    }
  }
  private class DhcpPacketProcessor implements PacketProcessor {

    @Override
    public void process(PacketContext context) {
      // Stop processing if the packet has been handled, since we
      // can't do any more to it.
      if (context.isHandled()) {
        return;
      }
      InboundPacket pkt = context.inPacket();
      Ethernet ethPkt = pkt.parsed();
      // ConnectPoint clientCp = pkt.receivedFrom();
      if (ethPkt == null) {
          return;
      }
      // setUpConnectivity(context, dstId);
      if (!(pkt.receivedFrom().deviceId().equals(dhcpServerCp.deviceId()))) {
        ConnectPoint clientCp = pkt.receivedFrom();
        setUpConnectivity(context, clientCp, dhcpServerCp);
        forwardPacketToDst(context, dhcpServerCp);
        // forwardPacketToDst(context, clientCp);
        log.info("ClientCp device id: {} Port: {}", pkt.receivedFrom().deviceId(), pkt.receivedFrom().port());
        // forwardPacketToDst(context, clientCp);
      } else {
        // ConnectPoint clientCp = pkt.receivedFrom();
        // setUpConnectivity(context, clientCp, dhcpServerCp);
        // forwardPacketToDst(context, clientCp);
        Key key = Key.of(dhcpServerCp.toString() + ethPkt.getDestinationMAC(), appId);
        PointToPointIntent intent = (PointToPointIntent) intentService.getIntent(key);
        ConnectPoint clientCp = intent.filteredEgressPoint().connectPoint();
        forwardPacketToDst(context, clientCp);
        log.info("ServerCp device id: {} Port: {}", pkt.receivedFrom().deviceId(), pkt.receivedFrom().port());
        // forwardPacketToDstMac(context, pkt);
      }

    }
  }
  private void setUpConnectivity(PacketContext context, ConnectPoint icp, ConnectPoint ecp) {
    InboundPacket pkt = context.inPacket();
    Ethernet ethPkt = pkt.parsed();
    TrafficSelector selector;
    TrafficTreatment treatment;

    selector = DefaultTrafficSelector.builder()
            .matchEthType(Ethernet.TYPE_IPV4)
            .matchIPProtocol(IPv4.PROTOCOL_UDP)
            .matchUdpSrc(TpPort.tpPort(UDP.DHCP_CLIENT_PORT))
            .matchUdpDst(TpPort.tpPort(UDP.DHCP_SERVER_PORT))
            .build();

    treatment = DefaultTrafficTreatment.builder()
            .setOutput(ecp.port())
            .build();
    // selector = DefaultTrafficSelector.emptySelector();
    // treatment = DefaultTrafficTreatment.emptyTreatment();
    Key key = Key.of(icp.toString() + ethPkt.getSourceMAC(), appId);
    PointToPointIntent intent = (PointToPointIntent) intentService.getIntent(key);
    // PointToPointIntent intent;
    // Key key;
    // key = Key.of(ethPkt.getSourceMAC().toString() + , appId);
    if (intent == null) {
      key = Key.of(icp.toString() + ethPkt.getSourceMAC(), appId);
      log.info("Intent is null");
      FilteredConnectPoint ingress = new FilteredConnectPoint(icp);
      FilteredConnectPoint egress = new FilteredConnectPoint(ecp);
      intent = PointToPointIntent.builder()
            .selector(selector)
            .treatment(treatment)
            .filteredIngressPoint(ingress)
            .filteredEgressPoint(egress)
            .priority(PRIORITY)
            .appId(appId)
            .key(key)
            .build();
      intentService.submit(intent);
      log.info("Intent {}, port {} => {}, port {} is submitted.", icp.deviceId(), icp.port(), ecp.deviceId(), ecp.port());
      selector = DefaultTrafficSelector.builder()
            .matchEthType(Ethernet.TYPE_IPV4)
            .matchIPProtocol(IPv4.PROTOCOL_UDP)
            .matchUdpSrc(TpPort.tpPort(UDP.DHCP_SERVER_PORT))
            .matchUdpDst(TpPort.tpPort(UDP.DHCP_CLIENT_PORT))
            .build();

      treatment = DefaultTrafficTreatment.builder()
            .setOutput(icp.port())
            .build();

      key = Key.of(ecp.toString() + ethPkt.getSourceMAC(), appId);
      intent = PointToPointIntent.builder()
            .selector(selector)
            .treatment(treatment)
            .filteredIngressPoint(egress)
            .filteredEgressPoint(ingress)
            .priority(PRIORITY)
            .appId(appId)
            .key(key)
            .build();
      intentService.submit(intent);
      log.info("Intent {}, port {} => {}, port {} is submitted.", ecp.deviceId(), ecp.port(), icp.deviceId(), icp.port());
    }
    // selector = DefaultTrafficSelector.builder()
    //         .matchEthType(Ethernet.TYPE_IPV4)
    //         .matchIPProtocol(IPv4.PROTOCOL_UDP)
    //         .matchUdpSrc(TpPort.tpPort(UDP.DHCP_SERVER_PORT))
    //         .matchUdpDst(TpPort.tpPort(UDP.DHCP_CLIENT_PORT))
    //         .build();

    // treatment = DefaultTrafficTreatment.builder()
    //         .setOutput(icp.port())
    //         .build();

    // intent = PointToPointIntent.builder()
    //         .selector(selector)
    //         .treatment(treatment)
    //         .filteredIngressPoint(egress)
    //         .filteredEgressPoint(ingress)
    //         .priority(PRIORITY)
    //         .appId(appId)
    //         .build();
    // intentService.submit(intent);
  }
  private void forwardPacketToDst(PacketContext context, ConnectPoint dst) {
    TrafficTreatment treatment = DefaultTrafficTreatment.builder().setOutput(dst.port()).build();
    OutboundPacket packet = new DefaultOutboundPacket(dst.deviceId(),
                                                      treatment, context.inPacket().unparsed());
    packetService.emit(packet);
    log.info("sending packet: {}", packet);
    log.info("Device ID: {}", dst.deviceId());
  }
  private void forwardPacketToDstMac(PacketContext context, InboundPacket pkt) {
    ConnectPoint dst = pkt.receivedFrom();
    TrafficTreatment treatment = DefaultTrafficTreatment.builder().setEthDst(pkt.parsed().getDestinationMAC()).build();
    OutboundPacket packet = new DefaultOutboundPacket(dst.deviceId(),
                                                      treatment, context.inPacket().unparsed());
    packetService.emit(packet);
    log.info("sending packet: {}", packet);
    // log.info("Device ID: {}", dst.deviceId());
  }
}
