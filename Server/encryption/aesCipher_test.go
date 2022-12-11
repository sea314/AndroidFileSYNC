package encryption_test

import (
	"Server/encryption"
	"crypto/rand"
	"reflect"
	"testing"
)

func TestAESKeyToBytes(t *testing.T) {
	c := new(encryption.AESCipher)
	c.Initialize()
	iv1, key1 := c.GetKey()
	b := encryption.AESKeyToBytes(iv1, key1)
	iv2, key2, err := encryption.BytesToAESKey(b)
	if(err!=nil){
		t.Error(err)
	}
	if(!reflect.DeepEqual(iv1, iv2) || !reflect.DeepEqual(key1, key2)){
		t.Error("key不一致")
	}
}


func TestAES(t *testing.T){
	c1 := new(encryption.AESCipher)
	c2 := new(encryption.AESCipher)
	plain := make([]byte, 1000)
	rand.Read(plain)

	c1.Initialize()
	c2.InitializeWithKey(c1.GetKey())
	
	encrypted := c2.Encrypt(plain)
	decrypted := c1.Decrypt(encrypted)
	if(!reflect.DeepEqual(plain ,decrypted)){
		t.Error("plainとdecrypted不一致")
	}
}

