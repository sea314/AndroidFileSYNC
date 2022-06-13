package connection

import (
	"Server/encryption"
	"errors"
	"fmt"
	"log"
	"net"
	"strconv"
	"strings"
)

type BroadCastParam struct {
}

func ClientConnectionRecieve(port int, password_digest string) {
	for _, ip := range getHostIPList() {
		go broadcastRecieve(ip, port, password_digest)
	}
}

func broadcastRecieve(ipAddr string, port int, password_digest string) {
	udpAddr := &net.UDPAddr{
		IP:   net.ParseIP(ipAddr),
		Port: port,
	}
	updLn, err := net.ListenUDP("udp", udpAddr)

	if err != nil {
		log.Print(err)
		return
	}
	defer updLn.Close()

	buf := make([]byte, 1024)
	log.Printf("Starting udp server...%s:%d\n", ipAddr, port)

	for {
		n, udpAddr, err := updLn.ReadFromUDP(buf)
		if err != nil {
			log.Println(err)
		}

		go respondBroadCast(udpAddr, buf[:n], password_digest)
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

func respondBroadCast(udpAddr_ *net.UDPAddr, data []byte, password_digest string) {
	msg := string(data)
	udpAddr := udpAddr_

	msg_digest, err := checkBroadCastData(msg, password_digest)
	if err != nil {
		return
	}

	sendBroadCastResponse(udpAddr.String(), msg_digest, password_digest)
}

func checkBroadCastData(msg string, password_digest string) (string, error) {
	index := strings.Index(msg, ":")
	if index == -1 {
		fmt.Println("parse error")
		return "", errors.New("parse error")
	}

	time, err := strconv.ParseInt(msg[0:index], 10, 64)
	if err != nil {
		fmt.Println("parse error", err.Error())
		return "", errors.New("parse error")
	}

	passWithTime := fmt.Sprintf("%d:%s", time, password_digest)
	msg_digest := encryption.Sha256EncodeToString([]byte(passWithTime))

	if msg_digest != msg[index+1:] {
		fmt.Println("auth error")
		return "", errors.New("auth error")
	}
	return msg_digest, nil
}

func sendBroadCastResponse(clientAddr string, msg_digest string, password_digest string) {
	tcpAddr, err := net.ResolveTCPAddr("tcp", clientAddr)
	if err != nil {
		log.Println("net.ResolveTCPAddr:", err.Error())
		return
	}

	connection, err := net.DialTCP("tcp", nil, tcpAddr)
	if err != nil {
		log.Println("net.DialTCP:", err.Error())
		return
	}
	defer connection.Close()

	msg := encryption.Sha256EncodeToString([]byte(msg_digest + password_digest))
	connection.Write([]byte(msg))
}
