package encryption_test

import (
	"Server/encryption"
	"crypto/rand"
	"os"
	"reflect"
	"testing"
)

func TestPublicKeyToBytes(t *testing.T){
	c := new(encryption.RSACipher) 
	err := c.Initialize()
	if(err!=nil){
		t.Error(err)
	}
	b, err := c.GetPublicKeyBytes()
	if(err!=nil){
		t.Error(err)
	}
	publicKey, err := encryption.BytesToPublicKey(b)
	if(err!=nil){
		t.Error(err)
	}
	if(!publicKey.Equal(c.GetPublicKey())){
		t.Error("TestPublicKeyToBytes: publickey不一致")
	}
}

func TestRSA(t *testing.T){
	c1 := new(encryption.RSACipher)
	c2 := new(encryption.RSACipher)
	plain := make([]byte, 200)
	rand.Read(plain)

	err := c1.Initialize()
	if(err!=nil){
		t.Error(err)
	}
	c2.InitializeWithPublicKey(c1.GetPublicKey())
	
	encrypted, err := c2.Encrypt(plain)
	if(err != nil){
		t.Error(err)
	}
	decrypted, err := c1.Decrypt(encrypted)
	if(err != nil){
		t.Error(err)
	}

	if(!reflect.DeepEqual(plain ,decrypted)){
		t.Error("TestRSA: plainとdecrypted不一致")
	}
}

func TestSaveRSA(t *testing.T){
	c1 := new(encryption.RSACipher)
	c2 := new(encryption.RSACipher)

	filePath := "testPrivateKey.key"

	err := c1.Initialize()
	if(err!=nil){
		t.Error(err)
	}
	err = c1.SavePrivateKey(filePath)
	if(err != nil){
		t.Error(err)
	}
	err = c2.InitializeWithPrivateKeyFile(filePath)
	if(err != nil){
		t.Error(err)
	}
	plain := make([]byte, 200)
	rand.Read(plain)

	encrypted, err := c2.Encrypt(plain)
	if(err != nil){
		t.Error(err)
	}
	decrypted, err := c1.Decrypt(encrypted)
	if(err != nil){
		t.Error(err)
	}

	if(!reflect.DeepEqual(plain ,decrypted)){
		t.Error("TestSaveRSA: plainとdecrypted不一致")
	}
	os.Remove(filePath)
}
