package handler

import (
	"Server/connection"
	"crypto/rand"

	"github.com/gorilla/sessions"
)

type RSACipher = connection.RSACipher
type AESCipher = connection.AESCipher

var rsaCipher *RSACipher
var publicKeyBytes []byte 
var store *sessions.CookieStore
const aesKeySize int = 256

func Initialize(_rsaCipher *RSACipher) (*sessions.CookieStore, error) {
	var err error

	// 公開鍵暗号初期化
	rsaCipher = _rsaCipher
	publicKeyBytes, err = rsaCipher.GetPublicKeyBytes()
	if err != nil {
		return nil, err
	}

	// セッション管理初期化
	key :=  make([]byte, aesKeySize/8)
	rand.Read(key)
	store = sessions.NewCookieStore(key)

	return store, nil
}
