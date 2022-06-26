#include "u8conv.hpp"

using namespace std;
using path = std::filesystem::path;

int u8conv::GetPrivateProfileInt(const u8string& appName, const u8string& keyName, int defaultInt, const path& fileName) {
	return ::GetPrivateProfileIntA(cstr(appName), cstr(keyName), defaultInt, cstr(fileName.u8string()));
}

u8string u8conv::GetPrivateProfileString(const u8string& appName, const u8string& keyName, const u8string& defaultStr, const path& fileName) {
	int bufferSize = 1000;
	DWORD size;
	char8_t* buffer = new char8_t[bufferSize];
	while (1) {
		size = ::GetPrivateProfileStringA(cstr(appName), cstr(keyName), cstr(defaultStr), (LPSTR)buffer, bufferSize, cstr(fileName.u8string())) + 1;
		if (bufferSize != size) {
			break;
		}
		bufferSize *= 2;
		delete[] buffer;
		buffer = new char8_t[bufferSize];
	}
	u8string str(buffer);
	delete[] buffer;
	return str;
}

BOOL u8conv::WritePrivateProfileString(const u8string& appName, const u8string& keyName, const u8string& value, const path& fileName) {
	return ::WritePrivateProfileStringA(cstr(appName), cstr(keyName), cstr(value), cstr(fileName.u8string()));
}

BOOL u8conv::WritePrivateProfileInt(const u8string& appName, const u8string& keyName, int value, const path& fileName) {
	return ::WritePrivateProfileStringA(cstr(appName), cstr(keyName), to_string(value).c_str(), cstr(fileName.u8string()));
}

u8string  u8conv::GetDlgItemText(HWND hDlg, int nIDDlgItem) {
	HWND hCtrl = GetDlgItem(hDlg, nIDDlgItem);
	int bufferSize = ::GetWindowTextLengthA(hCtrl) + 1;
	char8_t* buffer = new char8_t[bufferSize];
	::GetWindowTextA(hCtrl, (LPSTR)buffer, bufferSize);
	u8string str(buffer);
	delete[] buffer;
	return str;
}

LPCSTR u8conv::cstr(const u8string& str)
{
	return (LPCSTR)str.c_str();
}

path u8conv::GetModuleFileName()
{
	int bufferSize = MAX_PATH + 1;
	DWORD size;
	char8_t* buffer = new char8_t[bufferSize];
	while(1){
		size = GetModuleFileNameA(NULL, LPSTR(buffer), bufferSize) + 1;
		if (size != bufferSize) {
			break;
		}
		bufferSize *= 2;
		delete[] buffer;
		buffer = new char8_t[bufferSize];
	}
	path str(buffer);
	delete[] buffer;
	return str;
}

path u8conv::GetCurrentDirectory()
{
	int bufferSize = GetCurrentDirectoryA(0, NULL) + 1;
	char8_t* buffer = new char8_t[bufferSize];
	GetCurrentDirectoryA(bufferSize, LPSTR(buffer));
	path str(buffer);
	delete[] buffer;
	return str;
}

vector<path> u8conv::DragQueryFile(HDROP hDrop)
{
	int fileCount = DragQueryFileA(hDrop, -1, NULL, 0);
	vector<path> files(fileCount);
	int bufferSize = MAX_PATH + 1;
	char8_t* buffer = new char8_t[bufferSize];
	for (int i = 0; i < fileCount; i++) {
		int size = DragQueryFileA(hDrop, i, NULL, 0) + 1;
		if (size > bufferSize) {
			bufferSize = size;
			delete[] buffer;
			buffer = new char8_t[bufferSize];
		}
		DragQueryFileA(hDrop, i, (LPSTR)buffer, bufferSize);
		files[i] = u8string(buffer);
	}
	delete[] buffer;
	return files;
}

path u8conv::OpenFolderDialog(HWND hWnd, const path& defaultPath)
{
	path dirName;
	HRESULT hr = CoInitializeEx(NULL, COINIT_APARTMENTTHREADED |
		COINIT_DISABLE_OLE1DDE);
	if (SUCCEEDED(hr))
	{
		IFileOpenDialog* pFileOpen;

		// Create the FileOpenDialog object.
		hr = CoCreateInstance(CLSID_FileOpenDialog, NULL, CLSCTX_ALL,
			IID_IFileOpenDialog, reinterpret_cast<void**>(&pFileOpen));

		if (SUCCEEDED(hr))
		{
			// Show the Open dialog box.
			DWORD options;
			pFileOpen->GetOptions(&options);
			pFileOpen->SetOptions(options | FOS_PICKFOLDERS);

			hr = pFileOpen->Show(hWnd);

			// Get the file name from the dialog box.
			if (SUCCEEDED(hr))
			{
				IShellItem* pItem;
				hr = pFileOpen->GetResult(&pItem);
				if (SUCCEEDED(hr))
				{
					PWSTR pszFilePath;
					hr = pItem->GetDisplayName(SIGDN_FILESYSPATH, &pszFilePath);

					// Display the file name to the user.
					if (SUCCEEDED(hr))
					{
						dirName = WstrToUtf8(pszFilePath);

						MessageBoxW(hWnd, pszFilePath, L"File Path", MB_OK);
						CoTaskMemFree(pszFilePath);
					}
					pItem->Release();
				}
			}
			pFileOpen->Release();
		}
		CoUninitialize();
	}

	return dirName;
}

wstring u8conv::Utf8ToWstr(const u8string& str)
{
	int bufferSize = ::MultiByteToWideChar(CP_UTF8, 0, (LPCCH)str.c_str(), -1, NULL, 0) + 1;
	wchar_t* buffer = new wchar_t[bufferSize];
	::MultiByteToWideChar(CP_UTF8, 0, (LPCCH)str.c_str(), -1, buffer, bufferSize);
	return wstring(buffer);
}

u8string u8conv::WstrToUtf8(const wstring& str)
{
	int bufferSize = ::WideCharToMultiByte(CP_UTF8, 0, str.c_str(), -1, NULL, 0, NULL, NULL) + 1;
	char8_t* buffer = new char8_t[bufferSize];
	::WideCharToMultiByte(CP_UTF8, 0, str.c_str(), -1, (LPSTR)buffer, bufferSize, NULL, NULL);
	return u8string(buffer);
}

vector<char8_t> u8conv::makeNullSeparetedString(const vector<u8string>& strs)
{
	size_t len = 0;
	for (const auto& s : strs) {
		len += s.length();
		len++;
	}
	len++;

	vector<char8_t> buffer(len, u8'\0');
	len = 0;

	for (const auto& s : strs) {
		memcpy(buffer.data()+len, s.data(), s.length());
		len += s.length();
		len++;
	}
	return buffer;
}
