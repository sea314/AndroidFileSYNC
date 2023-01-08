package handler

import (
	"net/http"

	"github.com/labstack/echo/v4"
)

func CheckLogin(next echo.HandlerFunc) echo.HandlerFunc {
	return func(c echo.Context) error {
		values, err := sess.GetAndLock(c)
		if err != nil {
			return c.String(http.StatusForbidden, "please login")
		}
		
		if values["aesKey"] == nil {
			sess.Unlock(c)
			return c.String(http.StatusForbidden, "please login")
		}
		sess.Unlock(c)
		return next(c)
	}
}
