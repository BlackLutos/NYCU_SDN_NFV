# NYCU_SDN_NFV
For NYCU_SDN_NFV Course

```
bazel run onos-local -- clean debug
onos localhost
0x800 (0x0800) IPV4
0x806 (0x0806) ARP
```
```
sudo mn --custom=topo_310581027.py --topo=topo_part3_310581027 --controller=remote,ip=127.0.0.1,port=6653 --switch=ovs,protocols=OpenFlow14
```
```
curl -i -u onos:rocks -X POST -H 'Content-Type: application/json' -d @flow1.json 'http://localhost:8181/onos/v1/flows/of:0000000000000001'
```
```
http://192.168.152.130:8181/onos/v1/docs
```
