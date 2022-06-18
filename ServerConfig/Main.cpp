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

ServerMgr server;
Config config;

INT_PTR CALLBACK DialogProc(HWND hwnd, UINT msg, WPARAM wp, LPARAM lp);

int WINAPI WinMain(_In_ HINSTANCE hInst, _In_opt_  HINSTANCE hPrevInst, _In_ LPSTR lpCmdLine, _In_ int nShowCmd)
{
	config.Load();
	u8string commandLine = (char8_t*)GetCommandLineA();
	if (commandLine.find_first_of(u8"autostartup")) {
		if (config.autoRun) {
			server.run(config);
		}
	}

	while (1) {
		DialogBoxW(hInst, MAKEINTRESOURCEW(IDD_DIALOG1), NULL, DialogProc);
		config.Save();

		if (!server.isRunning()) {
			return 0;
		}
	}
}


INT_PTR CALLBACK DialogProc(HWND hDlg, UINT msg, WPARAM wp, LPARAM lp) {
	switch (msg) {
	case WM_INITDIALOG:
		EnableWindow(GetDlgItem(hDlg, IDC_BUTTON_SERVER_RUN), !server.isRunning());
		EnableWindow(GetDlgItem(hDlg, IDC_BUTTON_SERVER_STOP), server.isRunning());
		SetWindowTextA(hDlg, cstr(config.fileName.u8string()));

		SetDlgItemInt(hDlg, IDC_EDIT_PORT, config.port, FALSE);
		SetDlgItemTextA(hDlg, IDC_EDIT_PASSWORD, "****");
		Button_SetCheck(GetDlgItem(hDlg, IDC_CHECK_AUTOSTARTUP), config.autoRun);
		SetDlgItemTextA(hDlg, IDC_EDIT_PATH, cstr(config.saveDir));

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

		return TRUE;
	}

	case WM_COMMAND:
		switch (LOWORD(wp)) {
		case IDOK:
		{
			config.port = GetDlgItemInt(hDlg, IDC_EDIT_PORT, NULL, FALSE);
			u8string password = GetDlgItemText(hDlg, IDC_EDIT_PASSWORD);
			config.autoRun = Button_GetState(GetDlgItem(hDlg, IDC_CHECK_AUTOSTARTUP)) == BST_CHECKED;
			config.saveDir = GetDlgItemText(hDlg, IDC_EDIT_PATH);
			if (password != u8"****") {
				config.passwordDigest = sha256ToBase64String(password.c_str(), password.size());
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
			config.saveDir = GetDlgItemText(hDlg, IDC_EDIT_PATH);

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

			return TRUE;
		}
	}


	return FALSE;
}
