#!/usr/bin/python3

from mininet.topo import Topo
from mininet.net import Mininet
from mininet.log import setLogLevel
from mininet.node import RemoteController
from mininet.cli import CLI
from mininet.node import Node
from mininet.link import TCLink

class MyTopo( Topo ):

    def __init__( self ):
        Topo.__init__( self )

        h1 = self.addHost('h1')
        h2 = self.addHost('h2')
        h3 = self.addHost('h3')
        h4 = self.addHost('h4')
        h5 = self.addHost('h5')

        s1 = self.addSwitch('s1', switch='ovs', protocols='OpenFlow14')
        s2 = self.addSwitch('s2', switch='ovs', protocols='OpenFlow14')
        s3 = self.addSwitch('s3', switch='ovs', protocols='OpenFlow14')

        self.addLink(s1, h1)
        self.addLink(s1, s3)
        self.addLink(s2, h2)
        self.addLink(s2, h3)
        self.addLink(s3, h4)
        self.addLink(s3, h5)
        self.addLink(s3, s2)

def run():
    topo = MyTopo()
    net = Mininet(topo=topo, controller=None, link=TCLink)
    net.addController('c0', controller=RemoteController, ip='127.0.0.1', port=6653)

    net.start()

    # print("[+] Run DHCP server")
    # dhcp = net.getNodeByName('h5')
    # pidFile = '/run/dhcp-server-dhcpd.pid'
    # dhcp.cmdPrint('/usr/sbin/dhcpd 4 -pf %s -cf ./dhcpd.conf %s' % (pidFile, dhcp.defaultIntf()))

    CLI(net)
    # print("[-] Killing DHCP server")
    # dhcp.cmdPrint("kill -9 `cat %s`" % pidFile)
    net.stop()

if __name__ == '__main__':
    setLogLevel('info')
    run()
