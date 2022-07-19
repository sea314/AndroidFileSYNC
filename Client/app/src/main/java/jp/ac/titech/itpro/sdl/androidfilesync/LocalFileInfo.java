package jp.ac.titech.itpro.sdl.androidfilesync;

public class LocalFileInfo extends ServerFileInfo {
    String localPath;

    public LocalFileInfo(String localPath, long fileSize, long lastModified, boolean idDir) {
        super(localPathToServerPath(localPath), fileSize, lastModified, idDir);
        this.localPath = localPath;
    }

    public boolean approximatelyEqual(LocalFileInfo b) {
        return super.approximatelyEqual(b) && localPath.equals(b.localPath);
    }

    public static String localPathToServerPath(String localPath){
        final String localStorage = "^/storage/emulated/0/";
        final String sdStorage = "^/storage/[0-9A-F]{4}-[0-9A-F]{4}/";
        if(localPath.matches(localStorage+".*")){
            return localPath.replaceFirst(localStorage, "ストレージ/");
        }
        if(localPath.matches(sdStorage+".*")){
            return localPath.replaceFirst(sdStorage, "SDカード/");
        }
        return localPath;
    }
}
