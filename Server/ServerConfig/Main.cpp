#include <windows.h>
#include "resource.h"
#include <string>

using namespace std;

BOOL CALLBACK DialogProc(HWND hwnd, UINT msg, WPARAM wp, LPARAM lp);


int WINAPI WinMain(HINSTANCE hInst, HINSTANCE hPrevInst,
	PSTR lpCmdLine, int nCmdShow) {
	DialogBox(hInst, MAKEINTRESOURCE(IDD_DIALOG1), NULL, DialogProc);
	return 0;
}


BOOL CALLBACK DialogProc(HWND hWnd, UINT msg, WPARAM wp, LPARAM lp) {
	static HWND hEditLog;
	auto u8str = u8"ƒeƒXƒg";
	switch (msg) {
	case WM_INITDIALOG:
		return TRUE;

	case WM_CLOSE:
		EndDialog(hWnd, IDOK);
		return TRUE;


	case WM_COMMAND:
		switch (LOWORD(wp)) {
		case IDOK:
			EndDialog(hWnd, IDOK);
			return TRUE;

		case IDC_BUTTON_PATH:

			return TRUE;
		}
	}


	return FALSE;
}

