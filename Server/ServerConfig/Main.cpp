#include <windows.h>
#include "resource.h"
#include <string>
#include "u8conv.hpp"
#include <windowsx.h>

using namespace std;
using namespace u8conv;

INT_PTR CALLBACK DialogProc(HWND hwnd, UINT msg, WPARAM wp, LPARAM lp);


int WINAPI WinMain(HINSTANCE hInst, HINSTANCE hPrevInst,
	PSTR lpCmdLine, int nCmdShow) {
	DialogBoxW(hInst, MAKEINTRESOURCEW(IDD_DIALOG1), NULL, DialogProc);
	return 0;
}


INT_PTR CALLBACK DialogProc(HWND hDlg, UINT msg, WPARAM wp, LPARAM lp) {
	static HWND hEditLog;
	u8string fileName = u8"config.ini";
	switch (msg) {
	case WM_INITDIALOG:
	{
		int port = GetPrivateProfileInt(u8"Connection", u8"Port", 12345, fileName);
		u8string passwordDigest = GetPrivateProfileString(u8"Connection", u8"PasswordDigest", u8"", fileName);
		SetDlgItemInt(hDlg, IDC_EDIT_PORT, port, FALSE);
		SetDlgItemTextA(hDlg, IDC_EDIT_PASSWORD, "****");

		int autoRun = GetPrivateProfileInt(u8"Server", u8"AutoRun", 1, fileName);
		u8string saveDir = GetPrivateProfileString(u8"Server", u8"SaveDir", u8"Android", fileName);
		Button_SetCheck( GetDlgItem(hDlg, IDC_CHECK_AUTOSTARTUP), autoRun);
		SetDlgItemTextA(hDlg, IDC_EDIT_PATH, conv(saveDir));
		return TRUE;
	}

	case WM_CLOSE:
		EndDialog(hDlg, IDOK);
		return TRUE;


	case WM_COMMAND:
		switch (LOWORD(wp)) {
		case IDOK:
			EndDialog(hDlg, IDOK);
			return TRUE;

		case IDCANCEL:
			EndDialog(hDlg, IDCANCEL);
			return TRUE;

		case IDC_BUTTON_PATH:

			return TRUE;
		}
	}


	return FALSE;
}

