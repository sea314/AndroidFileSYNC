package encryption

import (
	"crypto/cipher"
)

var aesKeySize int = 256

type ARSCipher struct {
	initialVector []byte
	encryptCipher cipher.Block
	decryptCipher cipher.Block
}

func (c *ARSCipher) Decrypt() {
}

func (c *ARSCipher) Encrypt() {

}

func (c *ARSCipher) GenerateKey() {

}
