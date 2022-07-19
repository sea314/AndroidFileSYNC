package jp.ac.titech.itpro.sdl.androidfilesync;

public class ServerFileInfo {
    private static final int DELTA_LAST_MODIFIED = 2000;        // FAT32等のフォーマットでは更新日時は2秒単位なので最大2秒のズレが発生する
    public String serverPath;
    public long fileSize;
    public long lastModified;
    public boolean isDir;
    public ServerFileInfo(String serverPath, long fileSize, long lastModified, boolean idDir){
        this.serverPath = serverPath;
        this.fileSize = fileSize;
        this.lastModified = lastModified;
        this.isDir = idDir;
    }

    // 更新日時のズレを無視して同一かどうか
    public boolean approximatelyEqual(ServerFileInfo b) {
        ServerFileInfo a = this;
        if(!a.isDir && !b.isDir){
            if(a.fileSize == b.fileSize && a.serverPath.equals(b.serverPath)){
                if(a.lastModified + DELTA_LAST_MODIFIED > b.lastModified
                        && a.lastModified < b.lastModified + DELTA_LAST_MODIFIED){
                    return true;
                }
            }
        }
        else if(a.isDir && b.isDir && a.serverPath.equals(b.serverPath)){
            return true;
        }
        return false;
    }
}
