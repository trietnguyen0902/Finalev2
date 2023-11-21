package tdtu.android.musicappdemov2;

import android.content.Intent;

public interface ActionPlay {
    void btnPlayClicked();

    int onStartCommand(Intent intent, int flags, int startId);

    void btnPreviousClicked();
    void btnNextClicked();
}
