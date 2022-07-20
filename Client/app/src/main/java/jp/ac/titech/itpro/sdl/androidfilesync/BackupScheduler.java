package jp.ac.titech.itpro.sdl.androidfilesync;

import android.app.NotificationManager;
import android.app.Service;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.work.ListenableWorker;
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