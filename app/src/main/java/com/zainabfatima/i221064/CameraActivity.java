package com.zainabfatima.i221064;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.video.MediaStoreOutputOptions;
import androidx.camera.video.Quality;
import androidx.camera.video.QualitySelector;
import androidx.camera.video.Recorder;
import androidx.camera.video.Recording;
import androidx.camera.video.VideoCapture;
import androidx.camera.video.VideoRecordEvent;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.Map;

/**
 * Screen 9 - Camera. Tap the shutter for a photo, press and hold to record a short video.
 * Normally hands the capture to the story editor (10); with {@link #EXTRA_RESULT_MODE} it
 * just returns the captured Uri (used by Create post).
 */
public class CameraActivity extends AppCompatActivity {

    public static final String EXTRA_RESULT_MODE = "result_mode";
    public static final String RESULT_URI = "uri";
    private static final long MAX_VIDEO_MS = 10_000;

    private PreviewView previewView;
    private ProcessCameraProvider provider;
    private ImageCapture imageCapture;
    private VideoCapture<Recorder> videoCapture;
    private Recording recording;
    private boolean back = true, flashOn = false, resultMode, holding;
    private TextView timer;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private long recordStart;

    private final ActivityResultLauncher<Intent> editorLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) finish();
            });

    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), r -> {
                if (r.getResultCode() != RESULT_OK || r.getData() == null) return;
                java.util.ArrayList<String> uris = r.getData().getStringArrayListExtra(PictureSendActivity.RESULT_URIS);
                if (uris != null && !uris.isEmpty()) deliver(Uri.parse(uris.get(0)), isVideoUri(uris.get(0)));
            });

    private final ActivityResultLauncher<String[]> permissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(), (Map<String, Boolean> r) -> {
                if (cameraGranted()) startCamera();
                else {
                    NavHelper.toast(this, "Camera permission is needed to take a story");
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_screen);
        resultMode = getIntent().getBooleanExtra(EXTRA_RESULT_MODE, false);
        previewView = findViewById(R.id.camera_preview);
        timer = findViewById(R.id.recordTimer);

        NavHelper.click(this, R.id.close_button, this::finish);
        NavHelper.click(this, R.id.settings_button, () -> NavHelper.toast(this, "Tap for a photo, hold for a video"));
        NavHelper.click(this, R.id.flash_button, () -> {
            flashOn = !flashOn;
            if (imageCapture != null)
                imageCapture.setFlashMode(flashOn ? ImageCapture.FLASH_MODE_ON : ImageCapture.FLASH_MODE_OFF);
            NavHelper.toast(this, flashOn ? "Flash on" : "Flash off");
        });
        NavHelper.click(this, R.id.flip_button, () -> {
            back = !back;
            bindUseCases();
        });
        NavHelper.click(this, R.id.emoji_button, () -> NavHelper.toast(this, "Effects coming soon"));
        NavHelper.click(this, R.id.gallery_thumbnail, () -> galleryLauncher.launch(
                new Intent(this, PictureSendActivity.class)
                        .putExtra(PictureSendActivity.EXTRA_MULTI, false)
                        .putExtra(PictureSendActivity.EXTRA_VIDEO, !resultMode)));
        NavHelper.click(this, R.id.mode_text, () -> NavHelper.toast(this, "Pick or capture something first"));
        NavHelper.click(this, R.id.mode_live, () -> NavHelper.toast(this, "Live isn't available yet"));
        NavHelper.click(this, R.id.mode_story, () -> NavHelper.toast(this, "You're in Story mode"));
        NavHelper.click(this, R.id.mode_post, () -> {
            if (!resultMode) startActivity(new Intent(this, CreatePostActivity.class));
            finish();
        });
        NavHelper.click(this, R.id.mode_boomerang, () -> NavHelper.toast(this, "Boomerang isn't available yet"));

        setupShutter();

        if (cameraGranted()) startCamera();
        else permissionLauncher.launch(new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO});
    }

    private boolean cameraGranted() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean audioGranted() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean isVideoUri(String s) {
        return s.contains("/video/");
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> f = ProcessCameraProvider.getInstance(this);
        f.addListener(() -> {
            try {
                provider = f.get();
                bindUseCases();
            } catch (Exception e) {
                NavHelper.toast(this, "Camera unavailable");
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindUseCases() {
        if (provider == null) return;
        provider.unbindAll();
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());
        imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setFlashMode(flashOn ? ImageCapture.FLASH_MODE_ON : ImageCapture.FLASH_MODE_OFF)
                .build();
        Recorder recorder = new Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.SD))
                .build();
        videoCapture = VideoCapture.withOutput(recorder);
        CameraSelector sel = back ? CameraSelector.DEFAULT_BACK_CAMERA : CameraSelector.DEFAULT_FRONT_CAMERA;
        try {
            if (resultMode) provider.bindToLifecycle(this, sel, preview, imageCapture);
            else provider.bindToLifecycle(this, sel, preview, imageCapture, videoCapture);
        } catch (Exception e) {
            try {
                provider.unbindAll();
                provider.bindToLifecycle(this, sel, preview, imageCapture);
                videoCapture = null;
            } catch (Exception e2) {
                NavHelper.toast(this, "Couldn't open the camera");
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupShutter() {
        final View shutter = findViewById(R.id.shutter_button);
        final Runnable startVideo = () -> {
            holding = true;
            startRecording();
        };
        shutter.setOnTouchListener((v, ev) -> {
            switch (ev.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    holding = false;
                    if (!resultMode) handler.postDelayed(startVideo, 450);
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    handler.removeCallbacks(startVideo);
                    if (holding) stopRecording();
                    else if (ev.getActionMasked() == MotionEvent.ACTION_UP) takePhoto();
                    holding = false;
                    return true;
                default:
                    return true;
            }
        });
    }

    private void takePhoto() {
        if (imageCapture == null) return;
        ContentValues cv = new ContentValues();
        cv.put(MediaStore.MediaColumns.DISPLAY_NAME, "kinnect_" + System.currentTimeMillis());
        cv.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
        cv.put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/Kinnect");
        ImageCapture.OutputFileOptions opts = new ImageCapture.OutputFileOptions.Builder(
                getContentResolver(), MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cv).build();
        imageCapture.takePicture(opts, ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(ImageCapture.OutputFileResults r) {
                        if (r.getSavedUri() != null) deliver(r.getSavedUri(), false);
                    }

                    @Override
                    public void onError(ImageCaptureException e) {
                        NavHelper.toast(CameraActivity.this, "Couldn't take the photo");
                    }
                });
    }

    @SuppressLint("MissingPermission")
    private void startRecording() {
        if (videoCapture == null || recording != null) return;
        ContentValues cv = new ContentValues();
        cv.put(MediaStore.MediaColumns.DISPLAY_NAME, "kinnect_" + System.currentTimeMillis());
        cv.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4");
        cv.put(MediaStore.MediaColumns.RELATIVE_PATH, "Movies/Kinnect");
        MediaStoreOutputOptions out = new MediaStoreOutputOptions.Builder(
                getContentResolver(), MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
                .setContentValues(cv)
                .setFileSizeLimit(com.zainabfatima.i221064.util.ImageUtil.MAX_VIDEO_BYTES)
                .build();
        androidx.camera.video.PendingRecording pending = videoCapture.getOutput().prepareRecording(this, out);
        if (audioGranted()) pending = pending.withAudioEnabled();
        recordStart = System.currentTimeMillis();
        timer.setVisibility(View.VISIBLE);
        tickTimer();
        recording = pending.start(ContextCompat.getMainExecutor(this), event -> {
            if (event instanceof VideoRecordEvent.Finalize) {
                VideoRecordEvent.Finalize fin = (VideoRecordEvent.Finalize) event;
                recording = null;
                timer.setVisibility(View.GONE);
                handler.removeCallbacks(tick);
                Uri uri = fin.getOutputResults().getOutputUri();
                if (!fin.hasError() || fin.getError() == VideoRecordEvent.Finalize.ERROR_FILE_SIZE_LIMIT_REACHED) {
                    if (uri != null && !Uri.EMPTY.equals(uri)) deliver(uri, true);
                } else {
                    NavHelper.toast(this, "Couldn't record the video");
                }
            }
        });
    }

    private final Runnable tick = this::tickTimer;

    private void tickTimer() {
        long el = System.currentTimeMillis() - recordStart;
        timer.setText(String.format(java.util.Locale.US, "0:%02d", el / 1000));
        if (el >= MAX_VIDEO_MS) {
            stopRecording();
            return;
        }
        handler.postDelayed(tick, 250);
    }

    private void stopRecording() {
        if (recording != null) {
            recording.stop();
        }
    }

    private void deliver(Uri uri, boolean video) {
        if (resultMode) {
            setResult(RESULT_OK, new Intent().putExtra(RESULT_URI, uri.toString()));
            finish();
        } else {
            editorLauncher.launch(new Intent(this, StoryEditorActivity.class)
                    .putExtra(StoryEditorActivity.EXTRA_URI, uri.toString())
                    .putExtra(StoryEditorActivity.EXTRA_VIDEO, video));
        }
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (recording != null) recording.stop();
        super.onDestroy();
    }
}
