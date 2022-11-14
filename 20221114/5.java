/*
 * Copyright 2016-present Open Networking Foundation
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

package org.onosproject.net.neighbour;

import org.onosproject.net.Host;
import org.onosproject.net.host.HostService;

import java.util.Set;

import static org.onlab.packet.VlanId.vlanId;
import static org.onosproject.net.HostId.hostId;
/// 
import org.onlab.packet.IpAddress;

protected Map<IpAddress, MacAddress> arp_tables;
/**
 * Default neighbour message handler which implements basic proxying on an
 * L2 network (i.e. ProxyArp behaviour).
 */
public class DefaultNeighbourMessageHandler implements NeighbourMessageHandler {
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
                context.forward(context.dstMac());
                log.info("RECV REPLY. Requested MAC = {}", context.dstMac());
                break;
            default:
                break;
        }
        // case REPLY:
        //     Host h = hostService.getHost(hostId(context.packet().getDestinationMAC(),
        //             vlanId(context.packet().getVlanID())));

        //     if (h == null) {
        //         context.flood();
        //     } else {
        //         context.forward(h.location());
        //     }
        //     break;
        // case REQUEST:
        //     // See if we have the target host in the host store
        //     Set<Host> hosts = hostService.getHostsByIp(context.target());

        //     Host dst = null;
        //     Host src = hostService.getHost(hostId(context.srcMac(), context.vlan()));

        //     for (Host host : hosts) {
        //         if (host.vlan().equals(context.vlan())) {
        //             dst = host;
        //             break;
        //         }
        //     }

        //     if (src != null && dst != null) {
        //         // We know the target host so we can respond
        //         context.reply(dst.mac());
        //         return;
        //     }

        //     // The request couldn't be resolved.
        //     // Flood the request on all ports except the incoming port.
        //     context.flood();
        //     break;
        // default:
        //     break;
        //}

    }
}