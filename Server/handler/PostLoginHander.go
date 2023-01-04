package handler

import (
	"io"
	"log"
	"net/http"

	"github.com/labstack/echo/v4"
)

func PostLoginHander(c echo.Context) error {
	request := c.Request()
	sess.Create(c)
	values, err := sess.Get(c)
	if err != nil {
		return c.NoContent(http.StatusForbidden)
	}

	msgDigestBase64 := request.Header.Get("Sha-256")
	// Bodyデコード
	keyEncrypted, err := io.ReadAll(request.Body)
	if err != nil {
		return c.NoContent(http.StatusBadRequest)
	}
	key, err := rsaCipher.Decrypt(keyEncrypted)
	if err != nil {
		return c.NoContent(http.StatusBadRequest)
	}
	err = verifyMessage(msgDigestBase64, c.RealIP(), pwdDigestBase64, key)
	if err != nil {
		return c.NoContent(http.StatusForbidden)
	}

	aesCipher := new(AESCipher)
	err = aesCipher.InitializeWithKeyBytes(key)
	if err != nil {
		return c.NoContent(http.StatusForbidden)
	}
	values["aesKey"] = aesCipher
	sess.Set(c, values)
	log.Println("認証OK:", c.RealIP())
	return c.NoContent(http.StatusOK)
}
