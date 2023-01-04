package session

import (
	"errors"
	"time"

	"github.com/labstack/echo/v4"
)

// TODO: 同時にアクセスされないようにする
// 当面は1clientなので省略

type Session struct {
	lifeTime int64
	values   map[string]interface{}
}

type Sessions struct {
	sess map[string]Session
}

func (s *Sessions) Create(c echo.Context) {
	key := c.RealIP()
	if s.sess == nil{
		s.sess = make(map[string]Session)
	}
	s.sess[key] = Session{lifeTime: time.Now().Unix()+24*60*60, values: make(map[string]interface{})}
}

func (s *Sessions) Get(c echo.Context) (map[string]interface{}, error) {
	key := c.RealIP()
	sess, ok := s.sess[key]
	if(!ok){
		return nil, errors.New("Createを先に実行")
	}
	unixTime := time.Now().Unix()
	if sess.lifeTime < unixTime {
		s.unsafeErase(key)
		return nil, errors.New("有効期限切れ")
	}
	return sess.values, nil
}

func (s *Sessions) Set(c echo.Context, values map[string]interface{}) error {
	key := c.RealIP()
	sess, ok := s.sess[key]
	if(!ok){
		return errors.New("Createを先に実行")
	}
	unixTime := time.Now().Unix()
	if sess.lifeTime < unixTime {
		s.unsafeErase(key)
		return errors.New("有効期限切れ")
	}
	sess.values = values
	return nil
}


func (s *Sessions) Erase(c echo.Context) {
	s.unsafeErase(c.RealIP())
}

func (s *Sessions) unsafeErase(key string){
	delete(s.sess, key)
}
