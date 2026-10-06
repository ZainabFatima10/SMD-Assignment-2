package com.example.smd_assignment1;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

/** Screen 4 - Home feed. Hub that connects to every other screen. */
public class StartSocialMediaActivity extends AppCompatActivity {

    private boolean liked = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.start_social_media_screen);

        // ---- header + tab bar ----
        NavHelper.click(this, R.id.messageIcon, () -> open(ChatsActivity.class));
        NavHelper.click(this, R.id.btnSearch, () -> open(SearchActivity.class));

        NavHelper.click(this, R.id.tabHome, () -> NavHelper.toast(this, "You're on Home"));
        NavHelper.click(this, R.id.tabPeople, () -> open(FriendsActivity.class));
        NavHelper.click(this, R.id.tabShop, () -> open(MarketplaceActivity.class));
        NavHelper.click(this, R.id.tabNotif, () -> open(NotificationsActivity.class));
        NavHelper.click(this, R.id.tabMore, () -> open(MenuActivity.class));

        // ---- composer ----
        NavHelper.click(this, R.id.search_bar, () -> open(CreatePostActivity.class));
        NavHelper.click(this, R.id.btnImagePost, () -> open(PictureSendActivity.class));
        NavHelper.click(this, R.id.seagreenpfp, () -> open(MyProfileActivity.class));

        // ---- stories ----
        NavHelper.click(this, R.id.createStoryOption, () -> open(CameraActivity.class));
        NavHelper.click(this, R.id.story1, () -> open(StoryViewerActivity.class));   // Omar Farooq
        NavHelper.click(this, R.id.story2, () -> NavHelper.toast(this, "Sara Iqbal's story"));
        NavHelper.click(this, R.id.story3, () -> NavHelper.toast(this, "Hamza's story"));

        // ---- post ----
        NavHelper.click(this, R.id.pfp4, () -> NavHelper.toast(this, "Lina Marsh"));
        NavHelper.click(this, R.id.horizontal_more, () -> NavHelper.toast(this, "More options"));
        NavHelper.click(this, R.id.cancel, () -> NavHelper.toast(this, "Post hidden"));
        NavHelper.click(this, R.id.n1, () -> open(LikeActivity.class));        // open the post
        NavHelper.click(this, R.id.n2, () -> open(LikeActivity.class));
        NavHelper.click(this, R.id.n3, () -> open(LikeActivity.class));
        NavHelper.click(this, R.id.n4, () -> open(LikeActivity.class));
        NavHelper.click(this, R.id.reactions, () -> open(CommentsActivity.class));

        // Like: tap toggles, long-press opens the reaction picker (screen 5)
        Runnable toggleLike = () -> {
            liked = !liked;
            TextView word = findViewById(R.id.likeword);
            word.setTextColor(liked ? NavHelper.c("#0B5F63") : Color.parseColor("#8B898E"));
        };
        NavHelper.click(this, R.id.likeword, toggleLike);
        NavHelper.click(this, R.id.likeimg, toggleLike);
        NavHelper.longClick(this, R.id.likeword, () -> open(LikeActivity.class));
        NavHelper.longClick(this, R.id.likeimg, () -> open(LikeActivity.class));

        NavHelper.click(this, R.id.commentword, () -> open(CommentsActivity.class));
        NavHelper.click(this, R.id.commentimg, () -> open(CommentsActivity.class));
        NavHelper.click(this, R.id.shareword, () -> NavHelper.toast(this, "Share"));
        NavHelper.click(this, R.id.shareimg, () -> NavHelper.toast(this, "Share"));
    }

    private void open(Class<?> target) {
        startActivity(new Intent(this, target));
    }
}
