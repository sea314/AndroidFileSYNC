package handler

import (
	"os"
)



func Initialize(_rsaCipher *RSACipher) {
	pwdDigestBase64 = os.Getenv("AndroidFileSYNC PasswordDigest")
	rsaCipher = _rsaCipher
	sess = new(Sessions)
}
