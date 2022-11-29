all:
	sudo ovs-vsctl add-br ovs-br
	sudo ovs-vsctl set bridge ovs-br protocols=OpenFlow14
	sudo ifconfig ovs-br 172.20.0.1/16 up
	sudo ovs-vsctl set-controller ovs-br tcp:127.0.0.1:6653
	docker compose up -d
	sudo ovs-docker add-port ovs-br eth1 R1 --ipaddress=172.20.0.2/16
	sudo ovs-docker add-port ovs-br eth1 R2 --ipaddress=172.20.0.3/16
	sudo ovs-docker add-port ovs-br eth1 R3 --ipaddress=172.20.0.4/16
#	docker network create R3R4br
#	docker network connect R3R4br R3
#	docker network connect R3R4br R4
	docker ps -a
	sudo ovs-vsctl show
clean:
	sudo ovs-vsctl del-controller ovs-br
	sudo ovs-vsctl del-br ovs-br
	docker compose down
#	docker network rm R3R4br
#	sudo mn -c
	docker ps -a
	sudo ovs-vsctl show
#	sudo mn -c