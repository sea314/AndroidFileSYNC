// post /fileの処理ハンドラ

package handler

import (
	"encoding/base64"
	"errors"
	"fmt"
	"io"
	"net/http"
	"os"
	"os/user"
	"strconv"
	"strings"
	"syscall"

	"Server/encryption"
	"Server/winAPI"

	"github.com/labstack/echo/v4"
)

type PostFileParam struct {
	split        int
	path         string
	fileSize     uint64
	lastModified uint64
	fileData     []byte
	mode         string
}

func PostFileHandler(c echo.Context) error {
	param, err := parsePostFileParam(c.Request())
	if err != nil {
		return c.String(http.StatusBadRequest, err.Error())
	}

	if param.split == 0 {
		fmt.Println("path:", param.path)
		fmt.Println("fileSize:", param.fileSize)
		fmt.Println("lastModified:", param.lastModified)
		fmt.Println("mode:", param.mode)
	}

	err = writeFile(param)
	if err != nil {
		return c.String(http.StatusInternalServerError, err.Error())
	}
	return c.String(http.StatusOK, "ok")
}

func parsePostFileParam(request *http.Request) (PostFileParam, error) {
	// header読み込み
	var param PostFileParam
	var err1, err2, err3, err4, err5 error = nil, nil, nil, nil, nil
	var i64 int64
	param.split, err1 = strconv.Atoi(request.Header.Get("Split"))
	param.path = request.Header.Get("File-Path")
	i64, err2 = strconv.ParseInt(request.Header.Get("File-Size"), 10, 64)
	param.fileSize = uint64(i64)
	i64, err3 = strconv.ParseInt(request.Header.Get("Last-Modified"), 10, 64)
	param.lastModified = uint64(i64)
	param.mode = request.Header.Get("Mode")
	sha256 := request.Header.Get("Sha-256")

	if err1 != nil || err2 != nil || err3 != nil || err4 != nil || err5 != nil {
		return param, errors.New("parse header error")
	}

	// body読み込み
	bodyBuffer, err1 := io.ReadAll(request.Body)

	param.fileData, err2 = base64.StdEncoding.DecodeString(string(bodyBuffer))

	if sha256 != encryption.Sha256EncodeToString(param.fileData) || err1 != nil || err2 != nil {
		return param, errors.New("read body error")
	}
	return param, nil
}

func writeFile(param PostFileParam) error {
	var filepath string
	userData, err := user.Current()
	if err != nil {
		return errors.New("can't current user information")
	}
	split_path := strings.Split(param.path, "/")

	switch param.mode {
	case "DESKTOP":
		filepath = userData.HomeDir + "/Desktop/" + split_path[len(split_path)-1]

	case "BACKUP":
		dir := os.Getenv("AndroidFileSYNC BackupDirectory")
		os.MkdirAll(dir, os.ModeDir)
		filepath = dir + "/" + param.path
	}

	var file *os.File
	if param.split == 0 {
		var err error
		file, err = os.Create(filepath + ".tmp")
		if err != nil {
			return errors.New("can't create file:" + filepath + ".tmp")
		}
	} else { // 分割2回目以降
		var err error
		file, err = os.OpenFile(filepath+".tmp", os.O_WRONLY|os.O_APPEND, 0666)
		if err != nil {
			return errors.New("can't open file:" + filepath + ".tmp")
		}
	}
	file.Write(param.fileData)
	file.Close()

	if param.fileSize == 0 {
		os.Remove(filepath)
		os.Rename(filepath+".tmp", filepath)
		w_filepath, err := winAPI.StringToWCharPtr(filepath)
		if err != nil {
			return errors.New("can't set modified time" + filepath)
		}
		hFile, err := syscall.CreateFile(w_filepath, syscall.GENERIC_WRITE, 0, nil, syscall.OPEN_EXISTING, syscall.FILE_ATTRIBUTE_NORMAL, 0)
		modifiedTime := winAPI.UnixTimeToFileTime(param.lastModified)
		syscall.SetFileTime(hFile, nil, nil, &modifiedTime)
		syscall.CloseHandle(hFile)
	}
	return nil
}
