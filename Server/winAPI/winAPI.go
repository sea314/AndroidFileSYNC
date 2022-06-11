package winAPI

import (
	"syscall"
	"unsafe"

	"golang.org/x/sys/windows"
)

// winAPIとの連携用
func WCharPtrToString(p *uint16) string {
	return windows.UTF16PtrToString((*uint16)(unsafe.Pointer(p)))
}

func StringToWCharPtr(s string) (*uint16, error) {
	p, err := windows.UTF16PtrFromString(s)
	return (*uint16)(unsafe.Pointer(p)), err
}

func FileTimeToUnixTime(fileTime syscall.Filetime) uint64 {
	time := MakeInt64(fileTime.LowDateTime, fileTime.HighDateTime)
	time -= 116444412000000000 // PowerShell> (Get-Date "1970/1/1 0:0:0").ToFileTime()
	time /= 10000              // FileTime:100ns, UinxTime:1ms
	return time
}

func UnixTimeToFileTime(unixTime uint64) syscall.Filetime {
	time := unixTime * 10000   // FileTime:100ns, UinxTime:1ms
	time += 116444412000000000 // PowerShell> (Get-Date "1970/1/1 0:0:0").ToFileTime()
	var fileTime syscall.Filetime
	fileTime.HighDateTime = HighInt32(time)
	fileTime.LowDateTime = LowInt32(time)
	return fileTime
}

func HighInt32(value uint64) uint32 {
	return uint32((value >> 32) & 0xffffffff)
}

func LowInt32(value uint64) uint32 {
	return uint32(value & 0xffffffff)
}

func MakeInt64(lo uint32, hi uint32) uint64 {
	return uint64(lo) | (uint64(hi) << 32)
}
