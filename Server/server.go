package main

import (
	"Server/connection"
	"Server/handler"
	"fmt"
	"os"
	"strconv"

	"github.com/labstack/echo/v4"
)

func main() {
	for _, env := range os.Environ() {
		fmt.Println(env)
	}
	port, _ := strconv.Atoi(os.Getenv("AndroidFileSYNC Port"))
	passwordDigest := os.Getenv("AndroidFileSYNC PasswordDigest")

	go connection.ClientConnectionRecieve(port, passwordDigest)

	e := echo.New()

	e.POST("/file", handler.PostFileHandler)

	e.GET("/filelist", handler.GetFileListHandler)

	e.Logger.Fatal(e.Start(":" + strconv.Itoa(port)))
}
