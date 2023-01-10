package main

import (
	"Server/connection"
	"Server/handler"
	"os"
	"strconv"

	"github.com/labstack/echo/v4"
)


type RSACipher = connection.RSACipher


func main() {
	port, _ := strconv.Atoi(os.Getenv("AndroidFileSYNC Port"))
	pwdDigestBase64 := os.Getenv("AndroidFileSYNC PasswordDigest")
	directory := os.Getenv("AndroidFileSYNC BackupDirectory")
	os.MkdirAll(directory, os.ModeDir)

	rsaCipher := new(RSACipher)
	rsaCipher.Initialize()

	handler.Initialize(rsaCipher)

	e := echo.New()
//	e.Use(middleware.Logger())

	e.POST("/login", handler.PostLoginHander)


	withLogin := e.Group("")
	withLogin.Use(handler.CheckLogin)
	withLogin.POST("/file", handler.PostFileHandler)
	withLogin.GET("/filelist", handler.GetFileListHandler)
	withLogin.POST("/filedelete", handler.PostFileDeleteHandler)

	go connection.ClientConnectionRecieve(port, pwdDigestBase64, rsaCipher)

	e.Logger.Fatal(e.Start(":" + strconv.Itoa(port)))
}


