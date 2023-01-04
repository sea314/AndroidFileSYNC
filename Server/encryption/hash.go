package encryption

import (
	"crypto/sha256"
)

func Sha256Encode(bytes []byte) []byte {
	hash := sha256.Sum256(bytes)
	return hash[0:]
}
