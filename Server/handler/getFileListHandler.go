package handler

import (
	"fmt"
	"io/fs"
	"net/http"
	"os"
	"path/filepath"
	"strings"

	"github.com/labstack/echo/v4"
)

type FileInfo struct {
	path         string
	fileSize     uint64
	lastModified uint64
}

func GetFileListHandler(c echo.Context) error {
	fileList := make([]FileInfo, 0)
	directory := os.Getenv("AndroidFileSYNC BackupDirectory")

	filepath.Walk(directory,
		func(path string, info fs.FileInfo, err error) error {
			if path != directory {
				path = strings.TrimLeft(path, directory)
				fileList = append(fileList, FileInfo{path: path, fileSize: uint64(info.Size()), lastModified: uint64(info.ModTime().UnixMilli())})
			}

			return nil
		})
	fmt.Println(fileList)
	return c.String(http.StatusOK, "OK")
}
