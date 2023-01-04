package connection

import (
	"Server/encryption"
	"Server/handler"
	"encoding/base64"
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


func ClientConnectionRecieve(port int, pwdDigestBase64 string, rsaCipher *RSACipher) {
	for _, ip := range getHostIPList() {
		go broadcastRecieve(ip, port, pwdDigestBase64, rsaCipher)
	}
}

func broadcastRecieve(hostAddr string, port int, pwdDigestBase64 string, rsaCipher *RSACipher) {
	udpAddr := &net.UDPAddr{
		IP:   net.ParseIP(hostAddr),
		Port: port,
	}
	updLn, err := net.ListenUDP("udp", udpAddr)

	if err != nil {
		log.Print(err)
		return
	}
	defer updLn.Close()

	buf := make([]byte, 1024)
	log.Printf("Starting udp server...%s:%d\n", hostAddr, port)

	for {
		n, udpAddr, err := updLn.ReadFromUDP(buf)
		if err != nil {
			log.Println(err)
		}

		go respondBroadCast(udpAddr, hostAddr, buf[:n], pwdDigestBase64, rsaCipher)
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
					ip := ipnet.IP.String()
					if ip != "127.0.0.1"{
						ips = append(ips, ip)
					}
				}
			}
		}
	}
	return ips
}

func respondBroadCast(udpAddr *net.UDPAddr, hostAddr string, data []byte, pwdDigestBase64 string, rsaCipher *RSACipher) {
	msg := string(data)

	msgDigestBase64, err := checkBroadCastData(udpAddr, msg, pwdDigestBase64)
	if err != nil {
		return
	}

	sendBroadCastResponse(udpAddr, hostAddr, msgDigestBase64, pwdDigestBase64, rsaCipher)
}

func checkBroadCastData(udpAddr *net.UDPAddr, msg string, pwdDigestBase64 string) (msgDigestBase64 string, err error) {
	strs := strings.Split(msg, ",")
	if(len(strs) < 3 || strs[0] != "FileSYNC"){
		log.Println("parse error: incorrect format")
		return "", errors.New("parse error: incorrect format")
	}
	ip := udpAddr.IP.To4()
	clientIP := fmt.Sprintf("%d.%d.%d.%d", ip[0], ip[1], ip[2], ip[3])

	switch(strs[1]){
	case "0.1":
		if(len(strs) != 4){
			log.Println("parse error: incorrect format")
			return "", errors.New("parse error: incorrect format")
		}
		randomBytes, err := base64.RawURLEncoding.DecodeString(strs[2])
		if(err != nil){
			log.Println("parse error: "+err.Error())
			return "", errors.New("parse error: "+err.Error())
		}
		
		msgDigestBase64 = handler.MakeMessageHash(clientIP, pwdDigestBase64, randomBytes)
		if(msgDigestBase64 != strs[3]){
			log.Println("authentication error")
			return "", errors.New("authentication error")
		}
		return msgDigestBase64, nil

	default:
		log.Println("parse error: wrong version")
		return "", errors.New("parse error: wrong version")
	}
}

func sendBroadCastResponse(udpAddr *net.UDPAddr, hostAddr string, cilentMsgDigestBase64 string, pwdDigestBase64 string, rsaCipher *RSACipher) {
	tcpAddr, err := net.ResolveTCPAddr("tcp", udpAddr.String())
	if err != nil {
		log.Println("sendBroadCastResponse net.ResolveTCPAddr:", err.Error())
		return
	}

	connection, err := net.DialTCP("tcp", nil, tcpAddr)
	if err != nil {
		log.Println("sendBroadCastResponse net.DialTCP:", err.Error())
		return
	}
	defer connection.Close()

	publicKeyBytes, err := rsaCipher.GetPublicKeyBytes()
	if err != nil {
		log.Println("sendBroadCastResponse RSACipher.GetPublicKeyBytes:", err.Error())
		return
	}
	publicKeyBytesBase64 := base64.RawURLEncoding.EncodeToString(publicKeyBytes)

	msgDigestBase64 := handler.MakeMessageHash(hostAddr, pwdDigestBase64, []byte(cilentMsgDigestBase64), publicKeyBytes)

	// メッセージ本体　(アプリ名),(バージョン),(base64公開鍵),(base64メッセージハッシュ)
	msg := fmt.Sprintf("FileSYNC,0.1,%s,%s", publicKeyBytesBase64, msgDigestBase64)

	connection.Write([]byte(msg))
}
