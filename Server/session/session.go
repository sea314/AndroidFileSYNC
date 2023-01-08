package session

import (
	"errors"
	"sync"
	"time"

	"github.com/labstack/echo/v4"
)

type session struct {
	lifeTime int64
	values   map[string]interface{}
	mutex sync.Mutex
}

func newsession(lifeTime int64) *session {
	return &session{
		lifeTime: lifeTime, 
		values: make(map[string]interface{}),
		mutex: sync.Mutex{},
	}
}

type Sessions struct {
	sess sync.Map
	mutex sync.Mutex	// 同時createだけ防止、他はsync.Mapに任せる
}

func NewSessions() *Sessions {
	return &Sessions{
		sess: sync.Map{},
		mutex: sync.Mutex{},
	}
}

func (s *Sessions) Create(c echo.Context) {
	key := c.RealIP()
	s.create(key)
}

func (s *Sessions) create(key string){
	s.mutex.Lock()
	_, ok := s.sess.Load(key)
	if(!ok){	// ない場合だけ作る
		s.sess.Store(key, newsession(time.Now().Unix()+24*60*60))
	}
	s.mutex.Unlock()
}

func (s *Sessions) GetAndLock(c echo.Context) (map[string]interface{}, error) {
	key := c.RealIP()
	return s.getAndLock(key)
}

func (s *Sessions) getAndLock(key string) (map[string]interface{}, error) {
	raw, ok := s.sess.Load(key)
	if(!ok){
		return nil, errors.New("Createを先に実行")
	}
	sess := raw.(*session)
	unixTime := time.Now().Unix()
	if sess.lifeTime < unixTime {
		s.erase(key)
		return nil, errors.New("有効期限切れ")
	}
	sess.mutex.Lock()
	return sess.values, nil
}

func (s *Sessions) SetAndUnlock(c echo.Context, values map[string]interface{}) error {
	key := c.RealIP()
	return s.setAndUnlock(key, values)
}


func (s *Sessions) setAndUnlock(key string, values map[string]interface{}) error {
	raw, ok := s.sess.Load(key)
	if(!ok){
		return errors.New("Createを先に実行")
	}
	sess := raw.(*session)
	unixTime := time.Now().Unix()
	if sess.lifeTime < unixTime {
		s.erase(key)
		return errors.New("有効期限切れ")
	}
	sess.values = values
	sess.mutex.Unlock()
	return nil
}

func (s *Sessions) Unlock(c echo.Context){
	key := c.RealIP()
	s.unlock(key)
}


func (s *Sessions) unlock(key string){
	raw, ok := s.sess.Load(key)
	if(ok){
		sess := raw.(*session)
		sess.mutex.Unlock()
	}
}

func (s *Sessions) Erase(c echo.Context) {
	key := c.RealIP()
	s.erase(key)
}

func (s *Sessions) erase(key string) {
	s.sess.Delete(key)
}
