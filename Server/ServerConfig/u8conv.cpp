#include "u8conv.hpp"

int u8conv::GetPrivateProfileInt(const u8string& appName, const u8string& keyName, int defaultInt, const u8string& fileName) {
	return ::GetPrivateProfileIntA((LPCSTR)appName.c_str(), (LPCSTR)keyName.c_str(), defaultInt, (LPCSTR)fileName.c_str());
}

std::u8string u8conv::GetPrivateProfileString(const u8string& appName, const u8string& keyName, const u8string& defaultStr, const u8string& fileName) {
	char8_t* buffer = nullptr;
	int bufferSize = 1000;
	DWORD size = bufferSize - 1;
	for (; bufferSize - 1 == size; bufferSize *= 2) {
		delete[] buffer;
		buffer = new char8_t[bufferSize];
		size = GetPrivateProfileStringA((LPCSTR)appName.c_str(), (LPCSTR)keyName.c_str(), (LPCSTR)defaultStr.c_str(), (LPSTR)buffer, bufferSize, (LPCSTR)fileName.c_str());
	}
	u8string str(buffer);
	delete[] buffer;
	return str;
}

BOOL u8conv::WritePrivateProfileString(const u8string& appName, const u8string& keyName, const u8string& value, const u8string& filename) {
	return WritePrivateProfileStringA((LPCSTR)appName.c_str(), (LPCSTR)keyName.c_str(), (LPCSTR)value.c_str(), (LPCSTR)filename.c_str());
}

BOOL u8conv::WritePrivateProfileInt(const u8string& appName, const u8string& keyName, int value, const u8string& filename) {
	return WritePrivateProfileStringA((LPCSTR)appName.c_str(), (LPCSTR)keyName.c_str(), to_string(value).c_str(), (LPCSTR)filename.c_str());
}

LPCSTR u8conv::conv(const u8string& str)
{
	return (LPCSTR)str.c_str();
}

