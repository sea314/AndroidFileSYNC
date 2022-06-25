#include <windows.h>
#include "Config.hpp"
#include "u8conv.hpp"

using namespace u8conv;

Config::Config() {
	fileName = GetModuleFileName();
	//fileName.replace_filename(u8"\\config.ini");   Ç±ÇÃä÷êîÇÕÉoÉOÇ™óLÇÈ
	fileName = fileName.parent_path();
	fileName /= path(u8"config.ini");
}

void Config::Load() {
	port = GetPrivateProfileInt(u8"Connection", u8"Port", 12345, fileName);
	passwordDigest = GetPrivateProfileString(u8"Connection", u8"PasswordDigest", u8"", fileName);
	autoRun = GetPrivateProfileInt(u8"Server", u8"AutoRun", 0, fileName);
	saveDir = GetPrivateProfileString(u8"Server", u8"SaveDir", (GetModuleFileName().parent_path() / u8"Android").u8string(), fileName);
}


void Config::Save() {
	DWORD a = GetLastError();
	WritePrivateProfileInt(u8"Connection", u8"Port", port, fileName);
	WritePrivateProfileString(u8"Connection", u8"PasswordDigest", passwordDigest, fileName);
	WritePrivateProfileInt(u8"Server", u8"AutoRun", autoRun, fileName);
	WritePrivateProfileString(u8"Server", u8"SaveDir", saveDir, fileName);
	DWORD b = GetLastError();

	u8string commandLine = u8"\"" + GetModuleFileName().u8string() + u8"\"";
	commandLine += u8" autostartup";
	
	if (autoRun) {
		RegSetKeyValueA(HKEY_CURRENT_USER, R"(Software\Microsoft\Windows\CurrentVersion\Run)", "AndroidFileSYNC", REG_SZ, cstr(commandLine), commandLine.length() + 1);
	}
	else {
		RegDeleteKeyValueA(HKEY_CURRENT_USER, R"(Software\Microsoft\Windows\CurrentVersion\Run)", "AndroidFileSYNC");
	}
}
