#include "u8conv.hpp"

using namespace std;
using path = std::filesystem::path;

int u8conv::GetPrivateProfileInt(const u8string& appName, const u8string& keyName, int defaultInt, const path& fileName) {
	return ::GetPrivateProfileIntA(cstr(appName), cstr(keyName), defaultInt, cstr(fileName.u8string()));
}

u8string u8conv::GetPrivateProfileString(const u8string& appName, const u8string& keyName, const u8string& defaultStr, const path& fileName) {
	int bufferSize = 1000;
	DWORD size;
	char8_t* buffer = new char8_t[bufferSize];
	while (1) {
		size = ::GetPrivateProfileStringA(cstr(appName), cstr(keyName), cstr(defaultStr), (LPSTR)buffer, bufferSize, cstr(fileName.u8string()));
		if (bufferSize - 1 != size) {
			break;
		}
		bufferSize *= 2;
		delete[] buffer;
		buffer = new char8_t[bufferSize];
	}
	u8string str(buffer);
	delete[] buffer;
	return str;
}

BOOL u8conv::WritePrivateProfileString(const u8string& appName, const u8string& keyName, const u8string& value, const path& fileName) {
	return ::WritePrivateProfileStringA(cstr(appName), cstr(keyName), cstr(value), cstr(fileName.u8string()));
}

BOOL u8conv::WritePrivateProfileInt(const u8string& appName, const u8string& keyName, int value, const path& fileName) {
	return ::WritePrivateProfileStringA(cstr(appName), cstr(keyName), to_string(value).c_str(), cstr(fileName.u8string()));
}

u8string  u8conv::GetDlgItemText(HWND hDlg, int nIDDlgItem) {
	HWND hCtrl = GetDlgItem(hDlg, nIDDlgItem);
	int len = ::GetWindowTextLengthA(hCtrl);
	char8_t* buffer = new char8_t[len + 1];
	::GetWindowTextA(hCtrl, (LPSTR)buffer, len + 1);
	u8string str(buffer);
	delete[] buffer;
	return str;
}

LPCSTR u8conv::cstr(const u8string& str)
{
	return (LPCSTR)str.c_str();
}

path u8conv::GetModuleFileName()
{
	int bufferSize = MAX_PATH;
	DWORD size;
	char8_t* buffer = new char8_t[bufferSize];
	while(1){
		size = GetModuleFileNameA(NULL, LPSTR(buffer), bufferSize);
		if (size != bufferSize) {
			break;
		}
		bufferSize *= 2;
		delete[] buffer;
		buffer = new char8_t[bufferSize];
	}
	path str(buffer);
	delete[] buffer;
	return str;
}

