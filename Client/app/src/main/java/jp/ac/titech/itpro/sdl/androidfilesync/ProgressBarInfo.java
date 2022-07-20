package jp.ac.titech.itpro.sdl.androidfilesync;

public class ProgressBarInfo {
    public float progress;
    public float secondary_progress;
    public String message;
    public ProgressBarInfo(float progress, float secondary_progress, String message){
        this.progress = progress;
        this.secondary_progress = secondary_progress;
        this.message = message;
    }
}
