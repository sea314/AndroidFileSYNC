package encryption

import (
	"crypto/rand"
	"crypto/rsa"
	"crypto/x509"
	"io/ioutil"
	"os"

	"github.com/labstack/gommon/log"
)

const rsaKeySize int = 2048

type PrivateKey = rsa.PrivateKey
type PublicKey = rsa.PublicKey

type RSACipher struct {
	privateKey *PrivateKey
	publicKey  *PublicKey
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

func (c *RSACipher) InitializeWithPrivateKey(privateKey *PrivateKey) {
	c.privateKey = privateKey
	c.publicKey = &privateKey.PublicKey
}

func (c *RSACipher) InitializeWithPrivateKeyFile(filePath string) error {
	bytes, err := ioutil.ReadFile(filePath)
	if err != nil {
        return err
    }
	c.privateKey, err = BytesToPrivateKey(bytes)
	c.publicKey = &c.privateKey.PublicKey
	return err
}

func (c *RSACipher) InitializeWithPublicKey(publicKey *PublicKey) {
	c.privateKey = nil
	c.publicKey = publicKey
}

func (c *RSACipher) InitializeWithPublicKeyBytes(b []byte) error {
	publicKey, err := BytesToPublicKey(b)
	if(err != nil){
		return err
	}
	c.InitializeWithPublicKey(publicKey)
	return nil
}

func (c *RSACipher) Decrypt(ciphertext []byte) (out []byte, err error) {
	return rsa.DecryptPKCS1v15(rand.Reader, c.privateKey, ciphertext)
}

func (c *RSACipher) Encrypt(msg []byte) (out []byte, err error) {
	return rsa.EncryptPKCS1v15(rand.Reader, c.publicKey, msg)
}

func (c *RSACipher) GetPublicKey() *PublicKey {
	return c.publicKey
}

func (c *RSACipher) GetPrivateKey() *PrivateKey {
	return c.privateKey
}

func (c *RSACipher) GetPublicKeyBytes() ([]byte, error) {
	return PublicKeyToBytes(c.publicKey)
}

func (c *RSACipher) SavePrivateKey(filePath string) error {
	privBytes := PrivateKeyToByte(c.privateKey)
	f, err := os.Create(filePath)
	if err != nil{
		return err
	}
	_, err = f.Write(privBytes)
	if err != nil{
		return err
	}
	f.Close()
	return nil
}


// der形式で公開鍵をバイト列に変換
func PublicKeyToBytes(publicKey *PublicKey) ([]byte, error) {
	return x509.MarshalPKIXPublicKey(publicKey);
}

// der形式のバイト列を公開鍵に変換
func BytesToPublicKey(b []byte) (*PublicKey, error) {
	key, err := x509.ParsePKIXPublicKey(b)
	if(err != nil){return nil, err}
	return  key.(*PublicKey), nil
}

// der形式で秘密鍵をバイト列に変換
func PrivateKeyToByte(privateKey *PrivateKey) ([]byte){
	return x509.MarshalPKCS1PrivateKey(privateKey)
}

// der形式のバイト列を秘密に変換
func BytesToPrivateKey(b []byte) (*PrivateKey, error) {
	return x509.ParsePKCS1PrivateKey(b)
}

