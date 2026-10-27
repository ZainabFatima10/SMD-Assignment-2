package com.zainabfatima.i221064.feed;

import com.zainabfatima.i221064.model.Story;

import java.util.ArrayList;
import java.util.List;

/** All the live stories of one person, oldest first (the order they are played in). */
public class StoryGroup {
    public final String ownerId;
    public final List<Story> stories = new ArrayList<>();

    public StoryGroup(String ownerId) {
        this.ownerId = ownerId;
    }

    public long latest() {
        long t = 0;
        for (Story s : stories) t = Math.max(t, s.timestamp);
        return t;
    }

    public Story last() {
        return stories.isEmpty() ? null : stories.get(stories.size() - 1);
    }
}
