#include "ServerMgr.hpp"
#include "u8conv.hpp"


using namespace std;
using namespace u8conv;

HANDLE ServerMgr::hProcess;
HANDLE ServerMgr::hReadPipe;
u8string ServerMgr::serverLog;
thread ServerMgr::hThread;
mutex ServerMgr::mtx;

void ServerMgr::run(Config config) {
	if (isRunning()) {
		return;
	}

	// パイプ作成
	HANDLE hWritePipeTmp, hWritePipe;
	CreatePipe(&hReadPipe, &hWritePipeTmp, NULL, 0);

	// 子プロセスに継承させるために複製
	DuplicateHandle(GetCurrentProcess(), hWritePipeTmp, GetCurrentProcess(), &hWritePipe, 0, TRUE, DUPLICATE_SAME_ACCESS);
	CloseHandle(hWritePipeTmp);

	// 起動
	u8string commandLine = u8"Server.exe";
	commandLine += u8" ";
	commandLine += to_u8string(config.port);
	commandLine += u8" ";
	commandLine += config.passwordDigest;
	commandLine += u8" ";
	commandLine += config.saveDir;

	STARTUPINFOA startupInfo;
	PROCESS_INFORMATION processInfo;
	ZeroMemory(&startupInfo, sizeof(startupInfo));
	startupInfo.cb = sizeof(startupInfo);
	startupInfo.dwFlags = STARTF_USESTDHANDLES;
	startupInfo.hStdOutput = hWritePipe;
	startupInfo.hStdError = hWritePipe;
	ZeroMemory(&processInfo, sizeof(processInfo));

	CreateProcessA(NULL, (LPSTR)commandLine.data(), NULL, NULL, TRUE, CREATE_NO_WINDOW, NULL, NULL, &startupInfo, &processInfo);
	CloseHandle(processInfo.hThread);
	CloseHandle(hWritePipe);
	hProcess = processInfo.hProcess;

	hThread = thread(logRead);
}

void ServerMgr::stop() {
	if (hProcess != NULL) {
		TerminateProcess(hProcess, 0);
		hProcess = NULL;
	}
	if (hThread.joinable()) {
		hThread.detach();
	}
	if (hReadPipe != NULL) {
		CloseHandle(hReadPipe);
		hReadPipe = NULL;
	}
	if (hProcess != NULL) {
		CloseHandle(hProcess);
		hProcess = NULL;
	}
}

bool ServerMgr::isRunning() {
	if (hProcess == NULL) {
		return false;
	}

	DWORD exitCode = 0;
	GetExitCodeProcess(hProcess, &exitCode);
	if (exitCode != STILL_ACTIVE) {
		stop();
		return false;
	}
	return true;
}

u8string ServerMgr::getLog() {
	lock_guard lock(mtx);
	return serverLog;
}


void ServerMgr::logRead() {
	char8_t buffer[1000];
	u8string tmp;
	DWORD readBytes;
	while (ReadFile(hReadPipe, buffer, 1000, &readBytes, NULL)) {
		tmp = u8string(buffer, readBytes);
		int pos;

		tmp.find_first_of(u8"\n");

		lock_guard lock(mtx);
		serverLog += tmp;
		serverLog += u8"\r\n";
		while (serverLog.size() > 100000) {
			size_t i = serverLog.find_first_of(u8"\n");
			if (i == u8string::npos) { break; }
			serverLog = serverLog.substr(i + 1);
		}
	}
}

