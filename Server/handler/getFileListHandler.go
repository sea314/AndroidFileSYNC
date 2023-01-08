package handler

import (
	"encoding/json"
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

	_, _, err := requestDecrypter(c)
	if(err!=nil){
		return err
	}

	filepath.Walk(directory,
		func(path string, info fs.FileInfo, err error) error {
			if path != directory {
				relativePath, err := filepath.Rel(directory, path)
				if err != nil {
					log.Print("path parse error")
					log.Print(err.Error())
					return err
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
	bytes, err := json.Marshal(files)
	if err != nil{
		return c.String(http.StatusInternalServerError, err.Error())
	}
	body, err := responseEncrypter(c, bytes)
	if err != nil{
		return c.String(http.StatusInternalServerError, err.Error())
	}
	return c.Blob(http.StatusOK, "application/octet-stream", body)
}
