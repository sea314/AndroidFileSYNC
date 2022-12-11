package encryption

import (
	"bytes"
	"crypto/rand"
	"crypto/rsa"
	"encoding/base64"
	"encoding/binary"
	"errors"
	"math/big"

	"github.com/labstack/gommon/log"
)

const rsaKeySize int = 2048

type PrivateKey = rsa.PrivateKey
type PublicKey = rsa.PublicKey

type RSACipher struct {
	privateKey *PrivateKey
	publicKey  *PublicKey
}

func (c *RSACipher) Decrypt(ciphertext []byte) (out []byte, err error) {
	return rsa.DecryptPKCS1v15(rand.Reader, c.privateKey, ciphertext)
}

func (c *RSACipher) Encrypt(msg []byte) (out []byte, err error) {
	return rsa.EncryptPKCS1v15(rand.Reader, c.publicKey, msg)
}

func (c *RSACipher) Initialize() error {
	privateKey, err := rsa.GenerateKey(rand.Reader, rsaKeySize)
	if err != nil {
		log.Error(err)
		return err
	}
	c.privateKey = privateKey
	c.publicKey = &privateKey.PublicKey
	return nil
}

func (c *RSACipher) InitilazeWithPublicKey(publicKey *PublicKey) {
	c.privateKey = nil
	c.publicKey = publicKey
}

func (c *RSACipher) GetPublicKey() *PublicKey {
	return c.publicKey
}

func BytesToPublicKey(b []byte) (*PublicKey, error) {
	buf := bytes.NewBuffer(b)
	tmp := make([]byte, 8)
	var eSize int32
	var nSize int32
	var err error
	err = binary.Read(buf, binary.BigEndian, &tmp)
	if err != nil || string(tmp) != "ssh-rsa " { return nil, errors.New("形式が違います") }
	err = binary.Read(buf, binary.BigEndian, &eSize)
	if err != nil { return nil, errors.New("形式が違います") }
	eBytes := make([]byte, eSize)
	err = binary.Read(buf, binary.BigEndian, &eBytes)
	if err != nil { return nil, errors.New("形式が違います") }
	err = binary.Read(buf, binary.BigEndian, &nSize)
	if err != nil { return nil, errors.New("形式が違います") }
	nBase64 := make([]byte, nSize)
	err = binary.Read(buf, binary.BigEndian, &nBase64)
	if err != nil { return nil, errors.New("形式が違います") }
	
	publicKey := new(PublicKey)
	eInt := new(big.Int)
	eInt.SetBytes(eBytes)
	publicKey.E = int(eInt.Int64())
	nBytes, err := base64.StdEncoding.DecodeString(string(nBase64))
	if err != nil { return nil, err }
	nInt := new(big.Int)
	nInt.SetBytes(nBytes)
	publicKey.N = nInt
	return publicKey, nil
}

// OpenSSH形式で公開鍵をバイト列に変換
func PublicKeyToBytes(publicKey *PublicKey) []byte {
	// 形式
	// ssh-rsa (4バイトでEのバイト数)(E)(4バイトでNのバイト数)(Nのbase64エンコード)
	key_base64 := []byte(base64.StdEncoding.EncodeToString(publicKey.N.Bytes()))

	var buf bytes.Buffer
	binary.Write(&buf, binary.BigEndian, []byte("ssh-rsa ")) // go文字列の文字コードはutf-8なのでascii互換あり
	binary.Write(&buf, binary.BigEndian, int32(8))
	binary.Write(&buf, binary.BigEndian, int64(publicKey.E))
	binary.Write(&buf, binary.BigEndian, int32(len(key_base64)))
	binary.Write(&buf, binary.BigEndian, key_base64)
	return buf.Bytes()
}
