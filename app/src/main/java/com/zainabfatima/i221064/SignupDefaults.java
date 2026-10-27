package com.zainabfatima.i221064;

import com.zainabfatima.i221064.model.User;
import com.zainabfatima.i221064.util.Clock;
import com.zainabfatima.i221064.util.Db;

/** Writes a minimal /users record for an account whose profile is missing. */
final class SignupDefaults {

    private SignupDefaults() { }

    static User createMissingProfile(String uid, String email) {
        User u = new User();
        u.uid = uid;
        u.email = email;
        String local = email == null ? "" : email.split("@")[0];
        u.name = local.isEmpty() ? "Kinnect user" : local;
        u.nameLower = u.name.toLowerCase();
        u.profileComplete = false;
        u.createdAt = Clock.now();
        Db.user(uid).setValue(u);
        return u;
    }
}
