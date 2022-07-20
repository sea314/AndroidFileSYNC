package jp.ac.titech.itpro.sdl.androidfilesync;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class BackupScheduler extends Worker {
    private final static String TAG = BackupScheduler.class.getSimpleName();
    public BackupScheduler(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Config config = new Config();
        config.Load(getApplicationContext());
        SendFileService.startActionBackup(getApplicationContext(), config.getBackupPaths(), config.getPort(), config.getPasswordDigest());
        return Result.success();
    }
}