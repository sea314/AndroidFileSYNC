package encryption

import (
	"bytes"
	"crypto/aes"
	"crypto/cipher"
	"crypto/rand"
	"encoding/binary"
	"errors"
)

const aesKeySize int = 256

type AESCipher struct {
	initialVector []byte
	key []byte
	encrypter cipher.BlockMode
	decrypter cipher.BlockMode
}


func (c *AESCipher) Initialize() error {
	initialVector := make([]byte, aes.BlockSize)
	rand.Read(initialVector)
	key :=  make([]byte, aesKeySize/8)
	rand.Read(key)
	return c.InitializeWithKey(initialVector, key)
}

func (c *AESCipher) InitializeWithKey(initialVector []byte, key []byte) error{
	c.initialVector = initialVector 
	c.key = key
	block, err := aes.NewCipher(key)
	if err != nil { return err }
	c.encrypter = cipher.NewCBCEncrypter(block, initialVector)
	c.decrypter = cipher.NewCBCDecrypter(block, initialVector)
	return nil
}

func (c *AESCipher) InitializeWithKeyBytes(b []byte) error {
	iv, key, err := BytesToAESKey(b)
	if err!=nil { return err }
	return c.InitializeWithKey(iv, key)
}

func (c *AESCipher) Decrypt(ciphertext []byte) []byte {
	if len(ciphertext) == 0 {
		return []byte{}
	}
	if len(ciphertext) % aes.BlockSize != 0 {
		return nil
	}
	decrypted := make([]byte, len(ciphertext))
	c.decrypter.CryptBlocks(decrypted, ciphertext)
	return c.unpad(decrypted)
}

func (c *AESCipher) Encrypt(msg []byte) []byte {
	padded := c.pad(msg)
	encrypted := make([]byte, len(padded))
	c.encrypter.CryptBlocks(encrypted, padded)
	return encrypted
}

func (c *AESCipher) GetKey() (iv []byte, key []byte){
	return c.initialVector, c.key
} 

func (c *AESCipher) GetKeyBytes() []byte {
	return AESKeyToBytes(c.initialVector, c.key)
}

func AESKeyToBytes(iv []byte, key []byte) []byte{
	// 形式
	// (ivの長さ4バイト)(iv)(keyの長さ4バイト)(key)
	var buf bytes.Buffer
	binary.Write(&buf, binary.BigEndian, int32(len(iv)))
	binary.Write(&buf, binary.BigEndian, iv)
	binary.Write(&buf, binary.BigEndian, int32(len(key)))
	binary.Write(&buf, binary.BigEndian, key)
	return buf.Bytes()
}

func BytesToAESKey(b []byte) (iv []byte, key []byte, err error){
	buf := bytes.NewBuffer(b)
	var ivSize int32
	var keySize int32
	err = binary.Read(buf, binary.BigEndian, &ivSize)
	if err != nil { return nil, nil, errors.New("形式が違います") }
	iv = make([]byte, ivSize)
	err = binary.Read(buf, binary.BigEndian, &iv)
	if err != nil { return nil, nil, errors.New("形式が違います") }
	err = binary.Read(buf, binary.BigEndian, &keySize)
	if err != nil { return nil, nil, errors.New("形式が違います") }
	key = make([]byte, keySize)
	err = binary.Read(buf, binary.BigEndian, &key)
	if err != nil { return nil, nil, errors.New("形式が違います") }
	return iv, key, nil
}

// PKCS#7に従いパディング付与
func (c *AESCipher) pad(b []byte) []byte {
    padSize := aes.BlockSize - (len(b) % aes.BlockSize)
    pad := bytes.Repeat([]byte{byte(padSize)}, padSize)
    return append(b, pad...)
}

// PKCS#7に従いパディング削除
func (c *AESCipher) unpad(b []byte) []byte {
    padSize := int(b[len(b)-1])
    return b[:len(b)-padSize]
}
