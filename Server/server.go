package main

import (
	"Server/connection"
	"Server/handler"
	"fmt"
	"net/http"
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

	e.GET("/hello", func(c echo.Context) error {
		return c.String(http.StatusOK, "Hello World.\n")
	})

	e.GET("/sea314", func(c echo.Context) error {
		return c.String(http.StatusOK, "sea314(しーさんいちよん)です。\n")
	})

	e.GET("/ping", func(c echo.Context) error {
		return c.String(http.StatusOK, "pong\n")
	})

	e.Logger.Fatal(e.Start(":" + strconv.Itoa(port)))
}
