#pragma once
#include <windows.h>
#include <string>
#include <bcrypt.h>
#include <vector>

#pragma comment(lib,"Bcrypt.lib")
#pragma comment(lib,"Crypt32.lib")

namespace Encryption {
using namespace std;

vector<BYTE> sha256(const void* data, size_t size);
u8string sha256ToBase64String(const void* data, size_t size);
u8string base64EncodeToString(const void* data, size_t size);

}
