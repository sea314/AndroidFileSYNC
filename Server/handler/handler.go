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
	return base64.RawURLEncoding.EncodeToString(encryption.Sha256Encode(buf.Bytes()))
}

func requestDecrypter(c echo.Context, reqkeys... string) (body []byte, datas []string, err error) {
	request := c.Request()

	// 共通鍵取得
	values, err := sess.GetAndLock(c)
	if err != nil {
		return nil, nil, fmt.Errorf("sess.Get(c):%w", err)
	}
	defer sess.Unlock(c)
	aesCipher := values["aesKey"].(*AESCipher)
	if aesCipher == nil {
		return nil, nil, fmt.Errorf("aes key error")
	}

	// Headerのデコード
	datas = make([]string, len(reqkeys))
	decrypted := make([][]byte, len(reqkeys))
	for i, reqKey := range reqkeys {
		encryptedBase64 := request.Header.Get(reqKey)
		encrypted, err := base64.RawURLEncoding.DecodeString(encryptedBase64)
		if err != nil {
			return nil, nil, fmt.Errorf("base64.RawURLEncoding.DecodeString(encryptedBase64):%w", err)
		}
		decrypted[i] = aesCipher.Decrypt(encrypted)
		datas[i] = string(decrypted[i])
	}

	// Bodyデコード
	encrypted, err := io.ReadAll(request.Body)
	if err != nil {
		return nil, nil, fmt.Errorf("io.ReadAll(request.Body):%w", err)
	}
	body = aesCipher.Decrypt(encrypted)

	// データの確認
	msgDigestBase64 := request.Header.Get("Sha-256")
	decrypted = append(decrypted, body)
	return body, datas, verifyMessage(msgDigestBase64, c.RealIP(), pwdDigestBase64, decrypted...)
}

func responseEncrypter(c echo.Context, res []byte) ([]byte, error){
	// 共通鍵取得
	values, err := sess.GetAndLock(c)
	if err != nil {
		return nil, fmt.Errorf("sess.Get(c):%w", err)
	}
	defer sess.Unlock(c)
	aesCipher := values["aesKey"].(*AESCipher)
	if aesCipher == nil {
		return nil, fmt.Errorf("aes key error")
	}

	encrypted := aesCipher.Encrypt(res)
	hash := MakeMessageHash(c.RealIP(), pwdDigestBase64, res)
	return append(encrypted, []byte(hash)...), nil
}
