package connection

import (
	"fmt"
	"log"
	"net"
)

func ClientConnectionRecieve(port int) {
	broadcastRecieve("10.166.125.50", 12345)
	/*	for _, ip := range getHostIPList() {
		go broadcastRecieve(ip, port)
	}*/
}

func broadcastRecieve(ipAddr string, port int) {
	udpAddr := &net.UDPAddr{
		IP:   net.ParseIP(ipAddr),
		Port: port,
	}
	updLn, err := net.ListenUDP("udp", udpAddr)

	if err != nil {
		log.Print(err)
		return
	}

	buf := make([]byte, 1024)
	log.Printf("Starting udp server...%s:%d\n", ipAddr, port)

	for {
		n, udpAddr, err := updLn.ReadFromUDP(buf)
		if err != nil {
			log.Println(err)
		}

		go func() {
			log.Printf("Reciving data: %s from %s", string(buf[:n]), udpAddr.String())

			log.Printf("Sending data..")

			tcpAddr, err := net.ResolveTCPAddr("tcp", udpAddr.String())
			if err != nil {
				log.Println("net.ResolveTCPAddr:", err.Error())
				return
			}

			tcpConnection, err := net.DialTCP("tcp", nil, tcpAddr)
			if err != nil {
				log.Println("net.DialTCP:", err.Error())
				return
			}

			tcpConnection.Write([]byte("Pong\n"))
			log.Printf("Complete Sending data..")
		}()
	}
}

func getHostIPList() []string {
	ips := make([]string, 0)
	interfaces, err := net.Interfaces()
	if err != nil {
		fmt.Println(err)
		return nil
	}
	for _, inter := range interfaces {
		addrs, err := inter.Addrs()
		if err != nil {
			fmt.Println(err)
			return nil
		}
		for _, a := range addrs {
			if ipnet, ok := a.(*net.IPNet); ok {
				if ipnet.IP.To4() != nil {
					ips = append(ips, ipnet.IP.String())
				}
			}
		}
	}
	return ips
}
