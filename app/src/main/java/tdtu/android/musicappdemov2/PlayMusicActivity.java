package tdtu.android.musicappdemov2;

import static android.app.Service.START_NOT_STICKY;
import static tdtu.android.musicappdemov2.ApplicationClass.ACTION_NEXT;
import static tdtu.android.musicappdemov2.ApplicationClass.ACTION_PLAY;
import static tdtu.android.musicappdemov2.ApplicationClass.ACTION_PREVIOUS;
import static tdtu.android.musicappdemov2.ApplicationClass.CHANNEL_ID_2;
import static tdtu.android.musicappdemov2.MainActivity.randomBoolean;
import static tdtu.android.musicappdemov2.MainActivity.repeatBoolean;
import static tdtu.android.musicappdemov2.SongsAdapter.songsListAdapter;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioAttributes;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
//import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import android.view.MenuItem;
import android.widget.PopupMenu;
import android.widget.Toast;


public class PlayMusicActivity extends AppCompatActivity implements ActionPlay, ServiceConnection {
    private ImageButton btnPlay,btnMenuPlay, btnPrevious,btnNext, btnRepeat,btnRandom, btnBackMain;
    private TextView songStartTime, songEndTime, nameSong, author;
    private SeekBar progress_music;
    private ImageView songImg;
    private float playSpeed = 1.0f;

    private MediaPlayer mediaPlayer;
    private int position = -1;
    public static ArrayList<Songs> songsArrayList;
    private static Uri uri;
    private Handler handler = new Handler();
    private Thread playThread, nextThread, previousThread;
    MusicService musicService;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setFullScreen();
        setContentView(R.layout.activity_play_music);

        initView();
        getIntentMethod();


        progress_music.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(musicService != null && fromUser){
                    musicService.seekTo(progress * 1000);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        PlayMusicActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(musicService != null){
                    int currentPosition = musicService.getCurrentPosition() / 1000;
                    progress_music.setProgress(currentPosition);
                    songStartTime.setText(formattedTime(currentPosition));
                }
                handler.postDelayed(this,1000);
            }
        });

        btnRandom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(randomBoolean){
                    randomBoolean = false;
                    btnRandom.setImageResource(R.drawable.ic_swap_off_40dp);
                }else {
                    randomBoolean = true;
                    btnRandom.setImageResource(R.drawable.ic_swap_on_40dp);
                }
            }
        });

        mediaPlayer = new MediaPlayer();

        ImageButton btnMenuPlay = findViewById(R.id.btnMenuPlay);
        btnMenuPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showPopupMenu(view);
            }
        });

        btnRepeat.setOnClickListener(view -> {
            if(repeatBoolean){
                repeatBoolean = false;
                btnRepeat.setImageResource(R.drawable.ic_repeat_40dp);
            }else{
                repeatBoolean = true;
                btnRepeat.setImageResource(R.drawable.ic_repeat_one_40dp);
            }
        });

        btnBackMain.setOnClickListener(view -> {
            finish();
        });
    }
    private void initializeMediaPlayer() {
        mediaPlayer = new MediaPlayer();

        try {
            // Set the data source to your audio file path or URL
            mediaPlayer.setDataSource("your_audio_file_path_or_url");

            // Set any other configuration options if needed
            mediaPlayer.setAudioAttributes(
                    new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
            );

            // Set a listener for when the media is prepared
            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    // Media is prepared, you can start playing here if needed
                }
            });

            // Set a listener for when an error occurs
            mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    // Handle errors here
                    return false;
                }
            });

            // Set a listener for when playback is completed
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    // Handle completion here
                }
            });

            // Prepare the media asynchronously
            mediaPlayer.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
            // Handle exception
        }
    }
    private void setFullScreen() {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getSupportActionBar().hide();
    }


    @Override
    protected void onResume() {
        Intent intent = new Intent(this, MusicService.class);
        bindService(intent,this,BIND_AUTO_CREATE);

        playThreadBtn();
        previousThreadBtn();
        nextThreadBtn();
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unbindService(this);
    }

    private void nextThreadBtn() {
        nextThread = new Thread(){
            @Override
            public void run() {
                super.run();
                btnNext.setOnClickListener(view -> {
                    btnNextClicked();
                });
            }
        };
        nextThread.start();
    }

    public void btnNextClicked() {
        musicService.stop();
        musicService.release();

        if(randomBoolean && !repeatBoolean){
            position = getRandom(songsArrayList.size()-1);
        }else if (!randomBoolean && !repeatBoolean){
            position = ((position+1) % songsArrayList.size());
        }

        uri = Uri.parse(songsArrayList.get(position).getPath());
        musicService.createMediaPlayer(position);

        metaData(uri);
        handleSeekBar();
        musicService.onCompleted();
        btnPlay.setImageResource(R.drawable.ic_pause_80dp);

        if(!musicService.isPlaying()){
            musicService.start();
            startRotateSongImg();
            showNotification(R.drawable.ic_pause_80dp);
        }
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            int position = intent.getIntExtra("positionForService", -1);

            if (position != -1) {
                // Retrieve the URI of the selected song based on the position
                String songUri = PlayMusicActivity.songsArrayList.get(position).getPath();

                // Initialize and start the MediaPlayer
                initializeMediaPlayer(songUri);
            }
        }

        // Return the appropriate value based on your service requirements
        return START_NOT_STICKY;
    }

    private void initializeMediaPlayer(String songUri) {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }

        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioAttributes(
                new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
        );

        try {
            // Set the data source to the URI of the selected song
            mediaPlayer.setDataSource(songUri);
            mediaPlayer.prepare();
            mediaPlayer.start();

            // Add any other necessary logic, such as updating UI, handling playback controls, etc.
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Add a completion listener if you want to handle the end of playback
        mediaPlayer.setOnCompletionListener(mp -> {
            // Handle completion logic if needed
        });
}


    private int getRandom(int i) {
        Random random = new Random();
        return random.nextInt(i + 1);
    }

    private void previousThreadBtn() {
        previousThread = new Thread(){
            @Override
            public void run() {
                super.run();
                btnPrevious.setOnClickListener(view -> {
                    btnPreviousClicked();
                });
            }
        };
        previousThread.start();
    }

    public void btnPreviousClicked() {
        musicService.stop();
        musicService.release();

        if(randomBoolean && !repeatBoolean){
            position = getRandom(songsArrayList.size()-1);
        }else if (!randomBoolean && !repeatBoolean){
            position = ((position-1) < 0 ? (songsArrayList.size() - 1) : (position -1));
        }

        uri = Uri.parse(songsArrayList.get(position).getPath());
        musicService.createMediaPlayer(position);

        metaData(uri);
        handleSeekBar();

        musicService.onCompleted();
        btnPlay.setImageResource(R.drawable.ic_pause_80dp);

        if(!musicService.isPlaying()){
            musicService.start();
            startRotateSongImg();
            showNotification(R.drawable.ic_pause_80dp);
        }
    }

    private void playThreadBtn() {
        playThread = new Thread(){
            @Override
            public void run() {
                super.run();
                btnPlay.setOnClickListener(view -> {
                    btnPlayClicked();
                });
            }
        };
        playThread.start();
    }

    public void btnPlayClicked() {
        if(musicService.isPlaying()){
            showNotification(R.drawable.ic_play_80dp);
            btnPlay.setImageResource(R.drawable.ic_play_80dp);
            musicService.pause();
            stopRotateSongImg();
            handleSeekBar();
        }else {
            showNotification(R.drawable.ic_pause_80dp);
            btnPlay.setImageResource(R.drawable.ic_pause_80dp);
            musicService.start();
            startRotateSongImg();
            handleSeekBar();
        }
    }

    private String formattedTime(int currentPosition) {
        String total = "";
        String totalNew = "";
        String second = String.valueOf(currentPosition % 60);
        String minute = String.valueOf(currentPosition / 60);
        total = minute + ":" + second;
        totalNew = minute + ":0" + second;
        if (second.length() == 1){
            return totalNew;
        }
        return total;
    }

    private void getIntentMethod() {
        position = getIntent().getIntExtra("position",-1);
        songsArrayList = songsListAdapter;
        if(songsArrayList != null){
            btnPlay.setImageResource(R.drawable.ic_pause_80dp);
            uri = Uri.parse(songsArrayList.get(position).getPath());
        }
        showNotification(R.drawable.ic_pause_80dp);
        startRotateSongImg();
        Intent intent = new Intent(this,MusicService.class);
        intent.putExtra("positionForService", position);
        startService(intent);
    }

    private  void metaData(Uri uri) {
        nameSong.setText( songsArrayList.get( position ).getTitle() );
        author.setText( songsArrayList.get( position ).getArtist() );
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource( uri.toString() );
        int durationTotal = Integer.parseInt( songsArrayList.get( position ).getDuration() ) / 1000;
        songEndTime.setText( formattedTime( durationTotal ) );
        byte[] img = new byte[0];
        try {
            img = retriever.getEmbeddedPicture();
            // Process the image if it's not null
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            retriever.release();
        }

        if (img != null) {
            Glide.with( this ).asBitmap().load( img ).apply( new RequestOptions().override(
                    450, 500 ) ).into( songImg );
        } else {
            Glide.with( this ).asBitmap().load( R.drawable.default_image ).into( songImg );
        }
    }

    private void initView() {
        nameSong = findViewById(R.id.nameSongPlay);
        author = findViewById(R.id.authorSongPlay);
        songEndTime = findViewById(R.id.songEndTime);
        songStartTime = findViewById(R.id.songStartTime);

        btnPlay = findViewById(R.id.btnPlayPlay);
        btnPrevious = findViewById(R.id.btnPreviousPlay);
        btnNext = findViewById(R.id.btnNextPlay);
        btnRepeat = findViewById(R.id.btnRepeatPlay);
        btnRandom = findViewById(R.id.btnRandomPlay);
        btnBackMain = findViewById(R.id.btnBackMain);

        progress_music = findViewById(R.id.progress_music);

        songImg = findViewById(R.id.imgSongPlay);
    }


    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        MusicService.MusicBinder musicBinder = (MusicService.MusicBinder) iBinder;
        musicService = musicBinder.getService();
//        Toast.makeText(this,"Service connected" + musicService,Toast.LENGTH_LONG).show();
        musicService.setCallBackPlayerAction(this);
        progress_music.setMax(musicService.getDuration()/1000);
        metaData(uri);
        nameSong.setText(songsArrayList.get(position).getTitle());
        author.setText(songsArrayList.get(position).getArtist());
        musicService.onCompleted();
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        musicService = null;
    }

    public void showNotification(int btnPlay){
        Intent intent = new Intent(this,PlayMusicActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this,0,
                intent, PendingIntent.FLAG_IMMUTABLE);

        Intent prevIntent = new Intent(this,
                NotificationReceiver.class).setAction(ACTION_PREVIOUS);
        PendingIntent prevPending = PendingIntent.getBroadcast(this,0,
                prevIntent,PendingIntent.FLAG_UPDATE_CURRENT);

        Intent playIntent = new Intent(this,
                NotificationReceiver.class).setAction(ACTION_PLAY);
        PendingIntent playPending = PendingIntent.getBroadcast(this,0,
                playIntent,PendingIntent.FLAG_UPDATE_CURRENT);

        Intent nextIntent = new Intent(this,
                NotificationReceiver.class).setAction(ACTION_NEXT);
        PendingIntent nextPending = PendingIntent.getBroadcast(this,0,
                nextIntent,PendingIntent.FLAG_UPDATE_CURRENT);

        byte[] img = null;
        img = getSongImage(songsArrayList.get(position).getPath());
        Bitmap thumb = null;
        if(img != null){
            thumb = BitmapFactory.decodeByteArray(img,0,img.length);
        }else {
            thumb = BitmapFactory.decodeResource(getResources(),R.drawable.default_image);
        }

        String status;
        if(btnPlay == R.drawable.ic_pause_80dp){
            status = "Pause";
        }else{
            status = "Play";
        }

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID_2)
                .setSmallIcon(btnPlay)
                .setLargeIcon(thumb)
                .setContentTitle(songsArrayList.get(position).getTitle())
                .setContentText(songsArrayList.get(position).getArtist())
                .addAction(R.drawable.ic_previous_80dp,"Previous",prevPending)
                .addAction(btnPlay,status,playPending)
                .addAction(R.drawable.ic_next_80dp,"Next",nextPending)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOnlyAlertOnce(true)
                .build();
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        if (notificationManager != null){
            notificationManager.notify(0,notification);
            Log.e("notify success   ", "true");
        }else{
            Log.e("notify fail", "true");
        }
    }

    private byte[] getSongImage(String uri){
        MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
        mediaMetadataRetriever.setDataSource(uri);
        byte[] image = mediaMetadataRetriever.getEmbeddedPicture();
        mediaMetadataRetriever.release();
        return image;
    }

    private void handleSeekBar(){
        progress_music.setMax(musicService.getDuration() / 1000);
        PlayMusicActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(musicService != null){
                    int currentPosition = musicService.getCurrentPosition() / 1000;
                    progress_music.setProgress(currentPosition);
                }
                handler.postDelayed(this,1000);
            }
        });
    }

    private void startRotateSongImg(){
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                songImg.animate().rotationBy(360).withEndAction(this).setDuration(10*1000)
                        .setInterpolator(new LinearInterpolator()).start();
            }
        };
        songImg.animate().rotationBy(360).withEndAction(runnable).setDuration(10*1000)
                .setInterpolator(new LinearInterpolator()).start();
    }

    private void stopRotateSongImg(){
        songImg.animate().cancel();
    }



    private void showPopupMenu(View view) {
        PopupMenu popupMenu = new PopupMenu(this, view);
        popupMenu.inflate(R.menu.menu_play);

        // Set a listener on the menu items
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.action_speed_normal:
                        setPlaySpeed(1.0f);
                        return true;

                    default:
                        return false;
                }
            }
        });

        popupMenu.show();
    }


    private void setPlaySpeed(float speed) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // For Android API 23 and above, setPlaybackParams is available
            mediaPlayer.setPlaybackParams(mediaPlayer.getPlaybackParams().setSpeed(speed));
        } else {
            // For earlier versions, release and recreate MediaPlayer with the desired speed
            mediaPlayer.release();
            initializeMediaPlayer();
            mediaPlayer.setPlaybackParams(mediaPlayer.getPlaybackParams().setSpeed(speed));
        }
        playSpeed = speed;
        Toast.makeText(this, "Playback speed: " + playSpeed, Toast.LENGTH_SHORT).show();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}