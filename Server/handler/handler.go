// handlerで使う処理と変数を定義
package handler

import (
	"Server/encryption"
	"bytes"
	"encoding/base64"
	"encoding/binary"
	"errors"
	"fmt"
	"io"
	"net"

	"Server/session"

	"github.com/labstack/echo/v4"
)


type RSACipher = encryption.RSACipher
type AESCipher = encryption.AESCipher
type Sessions = session.Sessions

var rsaCipher *RSACipher
var sess *Sessions
const aesKeySize int = 256
var pwdDigestBase64 string


func verifyMessage(msgDigestBase64 string, ipAddr string, pwdDigestBase64 string, msgs... []byte) error {
	if(msgDigestBase64 == MakeMessageHash(ipAddr, pwdDigestBase64, msgs...)){
		return nil
	}
	return errors.New("tampering detected")
}

func MakeMessageHash(ipAddr string, pwdDigestBase64 string, msgs... []byte) string {
	var buf bytes.Buffer
	for _, data := range msgs {
		binary.Write(&buf, binary.BigEndian, data)
	}
	binary.Write(&buf, binary.BigEndian, []byte(net.ParseIP(ipAddr).String()))
	binary.Write(&buf, binary.BigEndian, []byte(pwdDigestBase64))
	return encryption.Sha256EncodeToString(buf.Bytes())
}

func httpDecoder(c echo.Context, reqkeys... string) (body []byte, datas [][]byte, err error) {
	request := c.Request()

	// 共通鍵取得
	values, err := sess.Get(c)
	if err != nil {
		return nil, nil, fmt.Errorf("sess.Get(c):%w", err)
	}
	aesCipher := values["aesKey"].(*AESCipher)
	if aesCipher == nil {
		return nil, nil, fmt.Errorf("aes key error")
	}

	// Headerのデコード
	datas = make([][]byte, len(reqkeys))
	for i, reqKey := range reqkeys {
		encryptedBase64 := request.Header.Get(reqKey)
		encrypted, err := base64.URLEncoding.DecodeString(encryptedBase64)
		if err != nil {
			return nil, nil, fmt.Errorf("base64.URLEncoding.DecodeString(encryptedBase64):%w", err)
		}
		datas[i] = aesCipher.Decrypt(encrypted)
	}

	// Bodyデコード
	encrypted, err := io.ReadAll(request.Body)
	if err != nil {
		return nil, nil, fmt.Errorf("io.ReadAll(request.Body):%w", err)
	}
	body = aesCipher.Decrypt(encrypted)

	// データの確認
	msgDigestBase64 := request.Header.Get("#Sha-256")
	datas2 := append(datas, body)
	return body, datas, verifyMessage(msgDigestBase64, c.RealIP(), pwdDigestBase64, datas2...)
}
