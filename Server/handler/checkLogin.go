package handler

import (
	"net/http"

	"github.com/labstack/echo/v4"
)

func CheckLogin(next echo.HandlerFunc) echo.HandlerFunc {
	return func(c echo.Context) error {
		values, err := sess.Get(c)
		if err != nil {
			return c.String(http.StatusForbidden, "please login")
		}
		
		if values["aesKey"] == nil {
			return c.String(http.StatusForbidden, "please login")
		}
		return next(c)
	}
}
