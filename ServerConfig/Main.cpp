#include <windows.h>
#include <windowsx.h>
#include <string>
#include <iostream>
#include "resource.h"
#include "u8conv.hpp"
#include "Encryption.hpp"
#include "ServerMgr.hpp"
#include "Config.hpp"

using namespace std;
using namespace u8conv;
using namespace Encryption;
using namespace std::filesystem;

ServerMgr server;
Config config;
u8string uniqueStr;


INT_PTR CALLBACK DialogProc(HWND hwnd, UINT msg, WPARAM wp, LPARAM lp);

int WINAPI WinMain(_In_ HINSTANCE hInst, _In_opt_  HINSTANCE hPrevInst, _In_ LPSTR lpCmdLine, _In_ int nShowCmd)
{
	bool visible = true;
	uniqueStr = u8"AndroidFileSYNC";

	// 多重起動チェック
	HANDLE hMSP = CreateMutexA(NULL, TRUE, cstr(uniqueStr));

	if (GetLastError() == ERROR_ALREADY_EXISTS) {
		// 既存プロセスに通知
		GlobalAddAtomA(cstr(uniqueStr));

		if (hMSP != NULL) {
			ReleaseMutex(hMSP);
			CloseHandle(hMSP);
		}
		return 0;
	}

	config.Load();
	u8string commandLine = (char8_t*)GetCommandLineA();
	if (commandLine.ends_with(u8"autostartup")) {
		if (config.autoRun) {
			server.run(config);
			visible = false;
		}
	}

	while (1) {
		if (visible) {
			DialogBoxW(hInst, MAKEINTRESOURCEW(IDD_DIALOG1), NULL, DialogProc);
			config.Save();
			visible = false;
		}
		if (!server.isRunning()) {
			if (hMSP != NULL) {
				ReleaseMutex(hMSP);
				CloseHandle(hMSP);
			}
			return 0;
		}

		if (GlobalFindAtomA(cstr(uniqueStr))) {
			GlobalDeleteAtom(GlobalFindAtomA(cstr(uniqueStr)));
			visible = true;
		}
		Sleep(100);
	}
}


INT_PTR CALLBACK DialogProc(HWND hDlg, UINT msg, WPARAM wp, LPARAM lp) {
	switch (msg) {
	case WM_INITDIALOG:
		EnableWindow(GetDlgItem(hDlg, IDC_BUTTON_SERVER_RUN), !server.isRunning());
		EnableWindow(GetDlgItem(hDlg, IDC_BUTTON_SERVER_STOP), server.isRunning());

		SetDlgItemInt(hDlg, IDC_EDIT_PORT, config.port, FALSE);
		if (config.passwordDigest == u8"") {
			SetDlgItemTextA(hDlg, IDC_EDIT_PASSWORD, "");
		}
		else {
			SetDlgItemTextA(hDlg, IDC_EDIT_PASSWORD, "****");
		}
		Button_SetCheck(GetDlgItem(hDlg, IDC_CHECK_AUTOSTARTUP), config.autoRun);
		SetDlgItemTextA(hDlg, IDC_EDIT_PATH, cstr(config.backupDir));



		SetTimer(hDlg, 0, 100 ,NULL);
		return TRUE;

	case WM_CLOSE:
		EndDialog(hDlg, IDOK);
		return TRUE;

	case WM_TIMER:
	{
		EnableWindow(GetDlgItem(hDlg, IDC_BUTTON_SERVER_RUN), !server.isRunning());
		EnableWindow(GetDlgItem(hDlg, IDC_BUTTON_SERVER_STOP), server.isRunning());

		u8string log = server.getLog();
		u8string log2 = GetDlgItemText(hDlg, IDC_EDIT_LOG);
		if (log.starts_with(log2)) {
			SendMessageA(GetDlgItem(hDlg, IDC_EDIT_LOG), EM_SETSEL, (WPARAM)log2.length(), (LPARAM)log2.length());
			SendMessageA(GetDlgItem(hDlg, IDC_EDIT_LOG), EM_REPLACESEL, 0, (LPARAM)log.c_str() + log2.length());
			SendMessageA(GetDlgItem(hDlg, IDC_EDIT_LOG), EM_SETSEL, (WPARAM)log.length(), (LPARAM)log.length());
		}
		else {
			SetDlgItemTextA(hDlg, IDC_EDIT_LOG, cstr(log));
			SendMessageA(GetDlgItem(hDlg, IDC_EDIT_LOG), EM_SETSEL, (WPARAM)log.length(), (LPARAM)log.length());
		}

		if (GlobalFindAtomA(cstr(uniqueStr))) {
			GlobalDeleteAtom(GlobalFindAtomA(cstr(uniqueStr)));
			SetForegroundWindow(hDlg);
		}
		return TRUE;
	}

	case WM_DROPFILES:
	{
		vector<path> files = DragQueryFile((HDROP)wp);
		if (files.size() > 0 && is_directory(files[0])) {
			SetDlgItemTextA(hDlg, IDC_EDIT_PATH, cstr(files[0].u8string()));
		}
		return TRUE;
	}

	case WM_COMMAND:
		switch (LOWORD(wp)) {
		case IDOK:
		{
			config.port = GetDlgItemInt(hDlg, IDC_EDIT_PORT, NULL, FALSE);
			u8string password = GetDlgItemText(hDlg, IDC_EDIT_PASSWORD);
			config.autoRun = Button_GetState(GetDlgItem(hDlg, IDC_CHECK_AUTOSTARTUP)) == BST_CHECKED;
			config.backupDir = GetDlgItemText(hDlg, IDC_EDIT_PATH);
			if (password != u8"****") {
				config.passwordDigest = sha256ToBase64String(password.c_str(), password.size());
			}
			if (password == u8"") {
				MessageBoxA(hDlg, cstr(u8"パスワードを設定してください"), cstr(u8"エラー"), MB_OK);
				return TRUE;
			}

			KillTimer(hDlg, 0);
			EndDialog(hDlg, IDOK);
			return TRUE;
		}

		case IDC_BUTTON_SERVER_RUN:
		{
			EnableWindow(GetDlgItem(hDlg, IDC_BUTTON_SERVER_RUN), FALSE);
			EnableWindow(GetDlgItem(hDlg, IDC_BUTTON_SERVER_STOP), FALSE);
			
			config.port = GetDlgItemInt(hDlg, IDC_EDIT_PORT, NULL, FALSE);
			u8string password = GetDlgItemText(hDlg, IDC_EDIT_PASSWORD);
			config.autoRun = Button_GetState(GetDlgItem(hDlg, IDC_CHECK_AUTOSTARTUP)) == BST_CHECKED;
			config.backupDir = GetDlgItemText(hDlg, IDC_EDIT_PATH);

			if (password != u8"****") {
				config.passwordDigest = sha256ToBase64String(password.c_str(), password.size());
			}
			server.run(config);
			return TRUE;
		}

		case IDC_BUTTON_SERVER_STOP:
		{
			EnableWindow(GetDlgItem(hDlg, IDC_BUTTON_SERVER_RUN), FALSE);
			EnableWindow(GetDlgItem(hDlg, IDC_BUTTON_SERVER_STOP), FALSE);

			server.stop();
			return TRUE;
		}

		case IDC_BUTTON_PATH:
		{
			path dirPath = OpenFolderDialog(hDlg, u8"");
			SetDlgItemTextA(hDlg, IDC_EDIT_PATH, cstr(dirPath.u8string()));
			return TRUE;
		}
		}
	}


	return FALSE;
}
