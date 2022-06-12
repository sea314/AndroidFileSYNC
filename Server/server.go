package main

import (
	"Server/connection"
	"Server/handler"
	"net/http"

	"github.com/labstack/echo/v4"
)

func main() {
	go connection.ClientConnectionRecieve(12345)

	e := echo.New()

	e.POST("/file", handler.PostFileHandler)

	e.GET("/hello", func(c echo.Context) error {
		return c.String(http.StatusOK, "Hello World.\n")
	})

	e.GET("/sea314", func(c echo.Context) error {
		return c.String(http.StatusOK, "sea314(しーさんいちよん)です。\n")
	})

	e.GET("/ping", func(c echo.Context) error {
		return c.String(http.StatusOK, "pong\n")
	})

	e.Logger.Fatal(e.Start(":12345"))
}
