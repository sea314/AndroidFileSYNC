package encryption

import (
	"crypto/sha256"
	"encoding/base64"
)

func Sha256EncodeToString(bytes []byte) string {
	cipher_bytes := sha256.Sum256(bytes)
	return base64.StdEncoding.EncodeToString(cipher_bytes[0:])
}
