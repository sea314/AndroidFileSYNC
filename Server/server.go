package main

import (
	"Server/connection"
	"Server/handler"
	"os"
	"strconv"

	"github.com/labstack/echo/v4"
)

func main() {
	port, _ := strconv.Atoi(os.Getenv("AndroidFileSYNC Port"))
	passwordDigest := os.Getenv("AndroidFileSYNC PasswordDigest")
	directory := os.Getenv("AndroidFileSYNC BackupDirectory")
	os.MkdirAll(directory, os.ModeDir)

	go connection.ClientConnectionRecieve(port, passwordDigest)

	e := echo.New()

	e.POST("/file", handler.PostFileHandler)

	e.GET("/filelist", handler.GetFileListHandler)

	e.Logger.Fatal(e.Start(":" + strconv.Itoa(port)))
}
