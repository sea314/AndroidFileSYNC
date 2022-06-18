#pragma once
#include <windows.h>
#include <thread>
#include <mutex>
#include "Config.hpp"

class ServerMgr {
	static HANDLE hProcess;
	static HANDLE hReadPipe;

	static std::u8string serverLog;

	static std::thread hThread;
	static std::mutex mtx;

public:
	static void run(Config config);
	static void stop();
	[[nodiscard]] static bool isRunning();
	[[nodiscard]] static std::u8string getLog();

private:
	static void logRead();
};



