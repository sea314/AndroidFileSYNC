package handler

import (
	"crypto/rand"
	"os"

	"github.com/gorilla/sessions"
)



func Initialize(_rsaCipher *RSACipher) (*sessions.CookieStore, error) {
	var err error

	pwdDigestBase64 = os.Getenv("AndroidFileSYNC PasswordDigest")

	// 公開鍵暗号初期化
	rsaCipher = _rsaCipher
	publicKeyBytes, err = rsaCipher.GetPublicKeyBytes()
	if err != nil {
		return nil, err
	}

	// セッション管理初期化
	key1 := make([]byte, 64)
	key2 := make([]byte, aesKeySize/8)
	rand.Read(key1)
	rand.Read(key2)
	store = sessions.NewCookieStore(key1, key2)

	return store, nil
}
