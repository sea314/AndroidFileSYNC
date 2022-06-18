#include "Encryption.hpp"

using namespace std;

vector<BYTE> Encryption::sha256(const void* data, size_t size) {
    BCRYPT_ALG_HANDLE       hAlg = NULL;
    BCRYPT_HASH_HANDLE      hHash = NULL;
    DWORD                   cbData = 0,
        cbHash = 0,
        cbHashObject = 0;
    PBYTE                   pbHashObject = NULL;
    PBYTE                   pbHash = NULL;

    //open an algorithm handle
    BCryptOpenAlgorithmProvider(&hAlg, BCRYPT_SHA256_ALGORITHM, NULL, 0);

    //calculate the size of the buffer to hold the hash object
    BCryptGetProperty(hAlg, BCRYPT_OBJECT_LENGTH, (PBYTE)&cbHashObject, sizeof(DWORD), &cbData, 0);

    //allocate the hash object on the heap
    pbHashObject = (PBYTE)HeapAlloc(GetProcessHeap(), 0, cbHashObject);

    //calculate the length of the hash
    BCryptGetProperty(hAlg, BCRYPT_HASH_LENGTH, (PBYTE)&cbHash, sizeof(DWORD), &cbData, 0);

    //allocate the hash buffer on the heap
    pbHash = (PBYTE)HeapAlloc(GetProcessHeap(), 0, cbHash);

    //create a hash
    BCryptCreateHash(hAlg, &hHash, pbHashObject, cbHashObject, NULL, 0, 0);

    //hash some data
    BCryptHashData(hHash, (PBYTE)data, size, 0);

    //close the hash
    BCryptFinishHash(hHash, pbHash, cbHash, 0);

    vector<BYTE> hash(cbHash);
    for (int i = 0; i < cbHash; i++) {
        hash[i] = pbHash[i];
    }

    if (hAlg)     BCryptCloseAlgorithmProvider(hAlg, 0);
    if (hHash)    BCryptDestroyHash(hHash);
    if (pbHashObject)   HeapFree(GetProcessHeap(), 0, pbHashObject);
    if (pbHash)   HeapFree(GetProcessHeap(), 0, pbHash);

    return hash;
}

u8string Encryption::sha256ToBase64String(const void* data, size_t size)
{
    auto hash = sha256(data, size);
    return base64EncodeToString(hash.data(), hash.size());
}

u8string  Encryption::base64EncodeToString(const void* data, size_t size)
{
    DWORD dwSizeBuffer = 0;
    CryptBinaryToStringA((BYTE*)data, size, CRYPT_STRING_BASE64 | CRYPT_STRING_NOCRLF, NULL, &dwSizeBuffer);
    char8_t* pszString = new char8_t[dwSizeBuffer];	// I’[•¶Žš—ñ•ª‘½‚­Šm•Û
    CryptBinaryToStringA((BYTE*)data, size, CRYPT_STRING_BASE64 | CRYPT_STRING_NOCRLF, (LPSTR)pszString, &dwSizeBuffer);
    u8string str(pszString);
    delete[] pszString;
    return str;
}

