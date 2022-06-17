#pragma once
#include <windows.h>
#include <string>


namespace u8conv {
using namespace std;

int GetPrivateProfileInt(u8string appName, u8string keyName, int defaultInt, u8string fileName){
	return ::GetPrivateProfileIntA((LPCSTR)appName.c_str(), (LPCSTR)keyName.c_str(), defaultInt, (LPCSTR)fileName.c_str());
}

u8string GetPrivateProfileString(u8string appName, u8string keyName, u8string defaultStr, u8string fileName) {
	char8_t* buffer = nullptr;
	int bufferSize = 1000;
	DWORD size = bufferSize - 1;
	for (;bufferSize == size - 1; bufferSize *= 2) {
		delete[] buffer;
		buffer = new char8_t[bufferSize];
		size = GetPrivateProfileStringA((LPCSTR)appName.c_str(), (LPCSTR)keyName.c_str(), (LPCSTR)defaultStr.c_str(), (LPSTR)buffer, bufferSize, (LPCSTR)fileName.c_str());
	}
	u8string str(buffer);
	delete[] buffer;
	return str;
}



}
