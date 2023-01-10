package handler

import (
	"encoding/json"
	"errors"
	"log"
	"net/http"
	"os"

	"github.com/labstack/echo/v4"
)

type PostFileDeleteParam struct {
	FileList []string `json:"fileList"`
}

func PostFileDeleteHandler(c echo.Context) error {
	param, err := parsePostFileDeleteParam(c)
	if err != nil {
		return c.String(http.StatusBadRequest, err.Error())
	}

	for i := 0; i < len(param.FileList); i++ {
		dir := os.Getenv("AndroidFileSYNC BackupDirectory")
		path := dir + "/" + param.FileList[i]
		log.Println("delete:", path)
		os.RemoveAll(path)
	}
	return c.NoContent(http.StatusOK)
}

func parsePostFileDeleteParam(c echo.Context) (PostFileDeleteParam, error) {
	// header読み込み
	var param PostFileDeleteParam

	body, _, err1 := requestDecrypter(c)
	if err1 != nil {
		log.Println(err1.Error())
		return param, errors.New("body read error")
	}

	if err := json.Unmarshal(body, &param); err != nil {
		log.Println(err.Error())
		return param, errors.New("parse error")
	}
	return param, nil
}
