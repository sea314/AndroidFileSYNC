// post /fileの処理ハンドラ

package handler

import (
	"errors"
	"log"
	"net/http"
	"os"
	"os/user"
	"path/filepath"
	"strconv"
	"strings"
	"syscall"

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
	param, err := parsePostFileParam(c)
	if err != nil {
		return c.String(http.StatusBadRequest, err.Error())
	}

	if param.split == 0 {
		log.Println("file recieved:", param.path)
	}

	err = writeFile(param)
	if err != nil {
		return c.String(http.StatusInternalServerError, err.Error())
	}
	return c.NoContent(http.StatusOK)
}

func parsePostFileParam(c echo.Context) (PostFileParam, error) {
	// header読み込み
	var param PostFileParam
	var err1, err2, err3, err4, err5 error = nil, nil, nil, nil, nil
	var i64 int64

	body, reqs, err := 
	requestDecrypter(c, "Split", "File-Path", "File-Size", "Last-Modified", "Mode")
	if err != nil{
		return param, err
	}

	param.split, err1 = strconv.Atoi(reqs[0])
	param.path = reqs[1]
	i64, err2 = strconv.ParseInt(reqs[2], 10, 64)
	param.fileSize = uint64(i64)
	i64, err3 = strconv.ParseInt(reqs[3], 10, 64)
	param.lastModified = uint64(i64)
	param.mode = reqs[4]
	param.fileData = body

	if err1 != nil || err2 != nil || err3 != nil || err4 != nil || err5 != nil {
		return param, errors.New("parse header error")
	}
	return param, nil
}

func writeFile(param PostFileParam) error {
	var filePath string
	userData, err := user.Current()
	if err != nil {
		return errors.New("can't current user information")
	}
	split_path := strings.Split(param.path, "/")

	switch param.mode {
	case "DESKTOP":
		filePath = userData.HomeDir + "/Desktop/" + split_path[len(split_path)-1]

	case "BACKUP":
		dir := os.Getenv("AndroidFileSYNC BackupDirectory")
		filePath = dir + "/" + param.path
		if param.split == 0 {
			if f, err := os.Stat(filePath); os.IsNotExist(err) || !f.IsDir(){
				os.MkdirAll(filepath.Dir(filePath), os.ModeDir)
			}
		}
	}

	var file *os.File
	if param.split == 0 {
		var err error
		file, err = os.Create(filePath + ".tmp")
		if err != nil {
			return errors.New("can't create file:" + filePath + ".tmp")
		}
	} else { // 分割2回目以降
		var err error
		file, err = os.OpenFile(filePath+".tmp", os.O_WRONLY|os.O_APPEND, 0666)
		if err != nil {
			return errors.New("can't open file:" + filePath + ".tmp")
		}
	}
	file.Write(param.fileData)
	file.Close()

	if param.fileSize == 0 {
		os.Remove(filePath)
		os.Rename(filePath+".tmp", filePath)
		w_filePath, err := winAPI.StringToWCharPtr(filePath)
		if err != nil {
			return errors.New("can't set modified time" + filePath)
		}
		hFile, err := syscall.CreateFile(w_filePath, syscall.GENERIC_WRITE, 0, nil, syscall.OPEN_EXISTING, syscall.FILE_ATTRIBUTE_NORMAL, 0)
		if err != nil {
			return errors.New("can't set modified time" + filePath)
		}
		modifiedTime := winAPI.UnixTimeToFileTime(param.lastModified)
		syscall.SetFileTime(hFile, nil, nil, &modifiedTime)
		syscall.CloseHandle(hFile)
	}
	return nil
}
