package handler

import (
	"log"
	"net/http"

	"github.com/labstack/echo-contrib/session"
	"github.com/labstack/echo/v4"
)

func CheckLogin(next echo.HandlerFunc) echo.HandlerFunc {
	return func(c echo.Context) error {
		sess, err := session.Get("sessions", c)
		if err != nil {
			log.Println("CheckLogin() session.Get:%w", err)
			return c.String(http.StatusInternalServerError, "something wrong in getting session")
		}
		
		if sess.Values["aesKey"] == nil {
			return c.String(http.StatusForbidden, "please login")
		}
		return next(c)
	}
}
