#pragma once
#include <windows.h>
#include <string>


namespace u8conv {
using namespace std;

[[nodiscard]] int GetPrivateProfileInt(const u8string& appName, const u8string& keyName, int defaultInt, const u8string& fileName);
[[nodiscard]] u8string GetPrivateProfileString(const u8string& appName, const u8string& keyName, const u8string& defaultStr, const u8string& fileName);
BOOL WritePrivateProfileString(const u8string& appName, const u8string& keyName, const u8string& value, const u8string& filename);
BOOL WritePrivateProfileInt(const u8string& appName, const u8string& keyName, int value, const u8string& filename);
LPCSTR conv(const u8string& str);
}
