package connection

import (
	"Server/encryption"
	"bytes"
	"encoding/base64"
	"encoding/binary"
	"errors"
	"fmt"
	"log"
	"net"
	"strings"
)

type PrivateKey = encryption.PrivateKey
type PublicKey = encryption.PublicKey
type RSACipher = encryption.RSACipher
type AESCipher = encryption.AESCipher


func ClientConnectionRecieve(port int, passwordDigest string) {
	for _, ip := range getHostIPList() {
		if(ip == "127.0.0.1"){		// ループバックアドレス
			continue;
		}
		go broadcastRecieve(ip, port, passwordDigest)
	}
}

func broadcastRecieve(ipAddr string, port int, passwordDigest string) {
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

		go respondBroadCast(udpAddr, buf[:n], passwordDigest)
	}
}

func getHostIPList() []string {
	ips := make([]string, 0)
	interfaces, err := net.Interfaces()
	if err != nil {
		log.Println(err)
		return nil
	}
	for _, inter := range interfaces {
		addrs, err := inter.Addrs()
		if err != nil {
			log.Println(err)
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

func respondBroadCast(udpAddr *net.UDPAddr, data []byte, passwordDigest string) {
	msg := string(data)

	msgDigest, _, err := checkBroadCastData(udpAddr, msg, passwordDigest)
	if err != nil {
		return
	}

	sendBroadCastResponse(udpAddr, msgDigest, passwordDigest)
}

func checkBroadCastData(udpAddr *net.UDPAddr, msg string, passwordDigest string) (msgDigest string, rsaCipher *RSACipher ,err error) {
	strs := strings.Split(msg, ",")
	if(len(strs) < 3 || strs[0] != "FileSYNC"){
		log.Println("parse error: incorrect format")
		return "", nil, errors.New("parse error: incorrect format")
	}
	ip := udpAddr.IP.To4()
	clientIP := fmt.Sprintf("%d.%d.%d.%d", ip[0], ip[1], ip[2], ip[3])

	switch(strs[1]){
	case "0.1":
		if(len(strs) != 5){
			log.Println("parse error: incorrect format")
			return "", nil, errors.New("parse error: incorrect format")
		}
		randomBytes, err := base64.StdEncoding.DecodeString(strs[2])
		if(err != nil){
			log.Println("parse error: "+err.Error())
			return "", nil, errors.New("parse error: "+err.Error())
		}
		publicKeyBytes, err := base64.StdEncoding.DecodeString(strs[3])
		if(err != nil){
			log.Println("parse error: "+err.Error())
			return "", nil, errors.New("parse error: "+err.Error())
		}
		rsaCipher = new(encryption.RSACipher)
		err = rsaCipher.InitializeWithPublicKeyBytes(publicKeyBytes)
		if(err != nil){
			log.Println("parse error: "+err.Error())
			return "", nil, errors.New("parse error: "+err.Error())
		}
		var buf bytes.Buffer
		binary.Write(&buf, binary.BigEndian, randomBytes)
		binary.Write(&buf, binary.BigEndian, []byte(passwordDigest))
		binary.Write(&buf, binary.BigEndian, publicKeyBytes)
		binary.Write(&buf, binary.BigEndian, []byte(clientIP))
		msgDigest := encryption.Sha256EncodeToString(buf.Bytes())
		if(msgDigest != strs[4]){
			log.Println("authentication error")
			return "", nil, errors.New("authentication error")
		}
		return msgDigest, rsaCipher, nil


	default:
		log.Println("parse error: wrong version")
		return "", nil, errors.New("parse error: wrong version")
	}
}

func sendBroadCastResponse(udpAddr *net.UDPAddr, msgDigest string, passwordDigest string) {
	tcpAddr, err := net.ResolveTCPAddr("tcp", udpAddr.String())
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

	msg := encryption.Sha256EncodeToString([]byte(msgDigest + passwordDigest))
	connection.Write([]byte(msg))
}
