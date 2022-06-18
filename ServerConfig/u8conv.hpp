#pragma once
#include <windows.h>
#include <string>
#include <filesystem>

namespace u8conv {
using namespace std;
using path = std::filesystem::path;

[[nodiscard]] int GetPrivateProfileInt(const u8string& appName, const u8string& keyName, int defaultInt, const path& fileName);
[[nodiscard]] u8string GetPrivateProfileString(const u8string& appName, const u8string& keyName, const u8string& defaultStr, const path& fileName);
BOOL WritePrivateProfileString(const u8string& appName, const u8string& keyName, const u8string& value, const path& filename);
BOOL WritePrivateProfileInt(const u8string& appName, const u8string& keyName, int value, const path& filename);

[[nodiscard]] u8string GetDlgItemText(HWND hDlg, int nIDDlgItem);

LPCSTR cstr(const u8string& str);

template <typename T>
[[nodiscard]] u8string to_u8string(T value) {
	return u8string((char8_t*)to_string(value).c_str());
}

[[nodiscard]] path GetModuleFileName();
}
