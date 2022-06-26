#pragma once
#include <string>
#include <filesystem>

struct Config {
	using u8string = std::u8string;
	using path = std::filesystem::path;
	path fileName;

	int port;
	u8string passwordDigest;
	int autoRun;
	u8string backupDir;

	Config();
	void Load();
	void Save();
};