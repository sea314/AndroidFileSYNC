package handler

import (
	"os"

	"Server/session"
)



func Initialize(_rsaCipher *RSACipher) {
	pwdDigestBase64 = os.Getenv("AndroidFileSYNC PasswordDigest")
	rsaCipher = _rsaCipher
	sess = session.NewSessions()
}
