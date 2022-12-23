package main

import (
	"Server/connection"
	"Server/handler"
	"fmt"
	"os"
	"strconv"

	"github.com/labstack/echo-contrib/session"
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

	store, err := handler.Initialize(rsaCipher)
	if(err != nil){
		fmt.Println(err)
		return
	}

	e := echo.New()
//	e.Use(middleware.Logger())
	e.Use(session.Middleware(store))

	e.POST("/file", handler.PostFileHandler)

	e.POST("/filedelete", handler.PostFileDeleteHandler)

	e.GET("/filelist", handler.GetFileListHandler)

	withLogin := e.Group("")
	withLogin.Use(handler.CheckLogin)

	go connection.ClientConnectionRecieve(port, pwdDigestBase64, rsaCipher)

	e.Logger.Fatal(e.Start(":" + strconv.Itoa(port)))
}


