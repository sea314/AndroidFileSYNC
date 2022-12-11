package encryption_test

import (
	"Server/encryption"
	"crypto/rand"
	"reflect"
	"testing"
)

func TestPublicKeyToBytes(t *testing.T){
	c := new(encryption.RSACipher) 
	c.Initialize()
	b, err := encryption.PublicKeyToBytes(c.GetPublicKey())
	if(err!=nil){
		t.Error(err)
	}
	publicKey, err := encryption.BytesToPublicKey(b)
	if(err!=nil){
		t.Error(err)
	}
	if(!publicKey.Equal(c.GetPublicKey())){
		t.Error("publickey不一致")
	}
}

func TestRSA(t *testing.T){
	c1 := new(encryption.RSACipher)
	c2 := new(encryption.RSACipher)
	plain := make([]byte, 200)
	rand.Read(plain)

	c1.Initialize()
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
		t.Error("plainとdecrypted不一致")
	}
}

