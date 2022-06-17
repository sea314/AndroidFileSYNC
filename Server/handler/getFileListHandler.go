package handler

import (
	"fmt"
	"io/fs"
	"net/http"
	"os/user"
	"path/filepath"

	"github.com/labstack/echo/v4"
)

type FileInfo struct {
	path         string
	fileSize     uint64
	lastModified uint64
}

func GetFileListHandler(c echo.Context) error {
	userData, _ := user.Current()
	fileList := make([]FileInfo, 0)

	filepath.Walk(userData.HomeDir+"/Desktop",
		func(path string, info fs.FileInfo, err error) error {
			fileList = append(fileList, FileInfo{path: path, fileSize: uint64(info.Size()), lastModified: uint64(info.ModTime().UnixMilli())})
			return nil
		})
	fmt.Println(fileList)
	return c.String(http.StatusOK, "OK")
}
