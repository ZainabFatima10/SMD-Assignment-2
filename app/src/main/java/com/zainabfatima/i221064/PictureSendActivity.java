package com.zainabfatima.i221064;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.zainabfatima.i221064.util.Async;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Screen 8 - Photo picker. Shows the device gallery, supports selecting several items (numbered
 * circles) and returns their Uris to the caller in {@link #RESULT_URIS}.
 */
public class PictureSendActivity extends AppCompatActivity {

    public static final String EXTRA_MULTI = "multi";
    public static final String EXTRA_MAX = "max";
    public static final String EXTRA_VIDEO = "video";
    public static final String RESULT_URIS = "uris";

    private static class Item {
        final Uri uri;
        final boolean video;
        Item(Uri u, boolean v) { uri = u; video = v; }
    }

    private final List<Item> items = new ArrayList<>();
    private final List<Uri> selected = new ArrayList<>();
    private boolean multi, allowVideo;
    private int max;

    private GalleryAdapter adapter;
    private ImageView preview;
    private TextView btnNext;

    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(),
                    (Map<String, Boolean> r) -> {
                        if (hasMediaAccess()) loadGallery();
                        else showNoPermission();
                    });

    private final ActivityResultLauncher<PickVisualMediaRequest> systemPicker =
            registerForActivityResult(new ActivityResultContracts.PickMultipleVisualMedia(10), uris -> {
                if (uris == null || uris.isEmpty()) return;
                ArrayList<String> out = new ArrayList<>();
                for (Uri u : uris) out.add(u.toString());
                finishWith(out);
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.picture_send_screen);

        multi = getIntent().getBooleanExtra(EXTRA_MULTI, true);
        max = getIntent().getIntExtra(EXTRA_MAX, 10);
        allowVideo = getIntent().getBooleanExtra(EXTRA_VIDEO, false);

        preview = findViewById(R.id.previewImage);
        btnNext = findViewById(R.id.most_relevant_text);
        if (!multi) findViewById(R.id.btnSelectMultiple).setVisibility(View.GONE);

        RecyclerView grid = findViewById(R.id.galleryGrid);
        grid.setLayoutManager(new GridLayoutManager(this, 4));
        adapter = new GalleryAdapter();
        grid.setAdapter(adapter);

        NavHelper.click(this, R.id.cancel_text, this::finish);
        NavHelper.click(this, R.id.most_relevant_text, this::confirm);
        NavHelper.click(this, R.id.btnSelectMultiple, () -> {
            multi = !multi;
            if (!multi && selected.size() > 1) {
                Uri first = selected.get(0);
                selected.clear();
                selected.add(first);
            }
            updateNext();
            adapter.notifyDataSetChanged();
        });
        NavHelper.click(this, R.id.btnSystemPicker, () -> systemPicker.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(allowVideo
                        ? ActivityResultContracts.PickVisualMedia.ImageAndVideo.INSTANCE
                        : ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build()));

        updateNext();
        if (hasMediaAccess()) loadGallery();
        else permissionLauncher.launch(requiredPermissions());
    }

    private String[] requiredPermissions() {
        List<String> p = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= 33) {
            p.add(Manifest.permission.READ_MEDIA_IMAGES);
            if (allowVideo) p.add(Manifest.permission.READ_MEDIA_VIDEO);
            if (Build.VERSION.SDK_INT >= 34) p.add("android.permission.READ_MEDIA_VISUAL_USER_SELECTED");
        } else {
            p.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        return p.toArray(new String[0]);
    }

    private boolean hasMediaAccess() {
        if (Build.VERSION.SDK_INT >= 33) {
            boolean images = granted(Manifest.permission.READ_MEDIA_IMAGES);
            boolean partial = Build.VERSION.SDK_INT >= 34 && granted("android.permission.READ_MEDIA_VISUAL_USER_SELECTED");
            return images || partial;
        }
        return granted(Manifest.permission.READ_EXTERNAL_STORAGE);
    }

    private boolean granted(String p) {
        return ContextCompat.checkSelfPermission(this, p) == PackageManager.PERMISSION_GRANTED;
    }

    private void showNoPermission() {
        findViewById(R.id.tvPermission).setVisibility(View.VISIBLE);
        findViewById(R.id.btnSystemPicker).setVisibility(View.VISIBLE);
    }

    private void loadGallery() {
        findViewById(R.id.tvPermission).setVisibility(View.GONE);
        findViewById(R.id.btnSystemPicker).setVisibility(View.GONE);
        Async.run(() -> query(), (list, err) -> {
            if (list == null) return;
            items.clear();
            items.addAll(list);
            adapter.notifyDataSetChanged();
            if (!items.isEmpty()) Glide.with(this).load(items.get(0).uri).into(preview);
        });
    }

    private List<Item> query() {
        List<Item> out = new ArrayList<>();
        ContentResolver cr = getContentResolver();
        Uri files = MediaStore.Files.getContentUri("external");
        String[] projection = {MediaStore.Files.FileColumns._ID, MediaStore.Files.FileColumns.MEDIA_TYPE};
        String selection = MediaStore.Files.FileColumns.MEDIA_TYPE + "=" + MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;
        if (allowVideo) selection += " OR " + MediaStore.Files.FileColumns.MEDIA_TYPE + "=" + MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO;
        try (Cursor c = cr.query(files, projection, selection, null, MediaStore.Files.FileColumns.DATE_ADDED + " DESC")) {
            if (c == null) return out;
            while (c.moveToNext() && out.size() < 300) {
                long id = c.getLong(0);
                boolean video = c.getInt(1) == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO;
                Uri base = video ? MediaStore.Video.Media.EXTERNAL_CONTENT_URI : MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                out.add(new Item(ContentUris.withAppendedId(base, id), video));
            }
        } catch (SecurityException ignored) { }
        return out;
    }

    private void toggle(Item item) {
        Glide.with(this).load(item.uri).into(preview);
        if (!multi) {
            selected.clear();
            selected.add(item.uri);
        } else if (selected.contains(item.uri)) {
            selected.remove(item.uri);
        } else if (selected.size() < max) {
            selected.add(item.uri);
        } else {
            NavHelper.toast(this, "You can pick up to " + max);
        }
        updateNext();
        adapter.notifyDataSetChanged();
    }

    private void updateNext() {
        btnNext.setText(selected.isEmpty() ? "Next" : "Next (" + selected.size() + ")");
    }

    private void confirm() {
        if (selected.isEmpty()) {
            NavHelper.toast(this, "Pick at least one item");
            return;
        }
        ArrayList<String> out = new ArrayList<>();
        for (Uri u : selected) out.add(u.toString());
        finishWith(out);
    }

    private void finishWith(ArrayList<String> uris) {
        Intent data = new Intent();
        data.putStringArrayListExtra(RESULT_URIS, uris);
        setResult(RESULT_OK, data);
        finish();
    }

    // ------------------------------------------------------------------ grid
    private class GalleryAdapter extends RecyclerView.Adapter<GalleryAdapter.Holder> {
        @NonNull @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_gallery, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder h, int position) {
            final Item item = items.get(position);
            Glide.with(h.thumb).load(item.uri).centerCrop().into(h.thumb);
            h.videoBadge.setVisibility(item.video ? View.VISIBLE : View.GONE);
            int index = selected.indexOf(item.uri);
            h.border.setVisibility(index >= 0 ? View.VISIBLE : View.GONE);
            h.number.setVisibility(index >= 0 ? View.VISIBLE : View.GONE);
            h.number.setText(String.valueOf(index + 1));
            h.itemView.setOnClickListener(v -> toggle(item));
        }

        @Override public int getItemCount() { return items.size(); }

        class Holder extends RecyclerView.ViewHolder {
            final ImageView thumb, videoBadge;
            final View border;
            final TextView number;
            Holder(View v) {
                super(v);
                thumb = v.findViewById(R.id.thumb);
                videoBadge = v.findViewById(R.id.videoBadge);
                border = v.findViewById(R.id.selectedBorder);
                number = v.findViewById(R.id.selectedNumber);
            }
        }
    }
}
