package handler

import (
	"io/fs"
	"log"
	"net/http"
	"os"
	"path/filepath"

	"github.com/labstack/echo/v4"
)

type Files struct {
	FileList []FileInfo `json:"fileList"`
}
type FileInfo struct {
	Path         string `json:"path"`
	FileSize     uint64 `json:"fileSize"`
	LastModified uint64 `json:"lastModified"`
	IsDir        bool   `json:"isDir"`
}

func GetFileListHandler(c echo.Context) error {
	fileList := make([]FileInfo, 0)
	directory := os.Getenv("AndroidFileSYNC BackupDirectory")

	filepath.Walk(directory,
		func(path string, info fs.FileInfo, err error) error {
			if path != directory {
				relativePath, err := filepath.Rel(directory, path)
				if err != nil {
					log.Print("path parse error")
					log.Print(err.Error())
					return nil
				}
				relativePath = filepath.ToSlash(relativePath)

				fileList = append(fileList, FileInfo{
					Path:         relativePath,
					FileSize:     uint64(info.Size()),
					LastModified: uint64(info.ModTime().UnixMilli()),
					IsDir:        info.IsDir(),
				})
			}

			return nil
		})

	files := Files{FileList: fileList}
	return c.JSON(http.StatusOK, files)
}
