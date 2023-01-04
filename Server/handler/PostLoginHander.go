package handler

import (
	"io"
	"net/http"

	"github.com/labstack/echo-contrib/session"
	"github.com/labstack/echo/v4"
	"github.com/labstack/gommon/log"
)

func PostLoginHander(c echo.Context) error {
	request := c.Request()
	sess, err := session.Get("sessions", c)
	if err != nil {
		log.Errorf("session.Get(\"sessions\", c):%w", err)
		return c.NoContent(http.StatusInternalServerError)
	}

	msgDigestBase64 := request.Header.Get("#Sha-256")
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
	sess.Values["aesKey"] = aesCipher
	sess.Save(c.Request(), c.Response())
	return c.NoContent(http.StatusOK)
}
