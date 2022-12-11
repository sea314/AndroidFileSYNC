package encryption

import (
	"crypto/rand"
	"crypto/rsa"
	"crypto/x509"

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

func (c *RSACipher) GetPublicKeyBytes() ([]byte, error) {
	return PublicKeyToBytes(c.publicKey)
}

// der形式で公開鍵をバイト列に変換
func PublicKeyToBytes(publicKey *PublicKey) ([]byte, error) {
	return x509.MarshalPKCS1PublicKey(publicKey), nil;
}

// der形式のバイト列を公開鍵に変換
func BytesToPublicKey(b []byte) (*PublicKey, error) {
	return x509.ParsePKCS1PublicKey(b)
}
