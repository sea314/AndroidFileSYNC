package session

import (
	"fmt"
	"strconv"
	"sync"
	"testing"
	"time"
)


func TestSession(t *testing.T) {
	sess := NewSessions()
	wg := new(sync.WaitGroup)
	wg.Add(20)
	for i:=0; i<10; i++ {
		go session_create_load_save(t, sess, "同一IP", i, wg)
	}
	for i:=0; i<10; i++ {
		go session_create_load_save(t, sess, "ip"+strconv.Itoa(i), i, wg)
	}
	wg.Wait()
}

func session_create_load_save(t *testing.T, sess *Sessions, key string, id int, wg *sync.WaitGroup){
	sess.create(key)
	value, err := sess.getAndLock(key)
	if err != nil{
		t.Error(err)
	}
	fmt.Println("key:",key," id:",id," locked")
	value[key] = "id="+strconv.Itoa(id)
	fmt.Println(value)
	time.Sleep(time.Microsecond*200)
	fmt.Println("key:",key," id:",id," release")
	sess.setAndUnlock(key, value)
	wg.Done()
}

