/*  Copyright (C) 2017-2024 Andreas Shimokawa, Carsten Pfeiffer, Daniele
    Gobbetti, José Rebelo, Lukas Veneziano, Maxim Baz

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.util;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Process;
import android.os.UserHandle;
import android.os.UserManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;

public class NotificationUtils {
    private static final Logger LOG = LoggerFactory.getLogger(NotificationUtils.class);

    @NonNull
    public static String getPreferredTextFor(NotificationSpec notificationSpec, int lengthBody, int lengthSubject, Context context) {
        switch (notificationSpec.type) {
            case GENERIC_ALARM_CLOCK:
                return StringUtils.getFirstOf(notificationSpec.title, notificationSpec.subject);
            case GENERIC_SMS:
            case GENERIC_EMAIL:
                return formatText(notificationSpec.sender, notificationSpec.subject, notificationSpec.body, lengthBody, lengthSubject, context);
            case GENERIC_NAVIGATION:
                return StringUtils.getFirstOf(notificationSpec.title, notificationSpec.body);
            case CONVERSATIONS:
            case FACEBOOK_MESSENGER:
            case GOOGLE_MESSENGER:
            case GOOGLE_HANGOUTS:
            case HIPCHAT:
            case KAKAO_TALK:
            case LINE:
            case RIOT:
            case SIGNAL:
            case WIRE:
            case SKYPE:
            case SNAPCHAT:
            case TELEGRAM:
            case THREEMA:
            case KONTALK:
            case ANTOX:
            case TWITTER:
            case WHATSAPP:
            case VIBER:
            case WECHAT:
                return StringUtils.ensureNotNull(notificationSpec.body);
        }
        return "";
    }

    @NonNull
    public static String formatSender(String sender, Context context) {
        if (sender == null || sender.length() == 0) {
            return "";
        }
        return context.getString(R.string.StringUtils_sender, sender);
    }


    @NonNull
    public static String formatText(String sender, String subject, String body, int lengthBody, int lengthSubject, Context context) {
        String fBody = StringUtils.truncate(body, lengthBody);
        String fSubject = StringUtils.truncate(subject, lengthSubject);
        String fSender = formatSender(sender, context);

        StringBuilder builder = StringUtils.join(" ", fBody, fSubject, fSender);
        return builder.toString().trim();
    }

    public static String getPreferredTextFor(CallSpec callSpec) {
        return StringUtils.getFirstOf(callSpec.name, callSpec.number);
    }

    @Nullable
    public static Drawable getAppIcon(final Context context, final String packageName) {
        try {
            return context.getPackageManager().getApplicationIcon(packageName);
        } catch (final PackageManager.NameNotFoundException ignored) {
            LOG.warn("Failed to find icon for {}, attempting fallback", packageName);
            final LauncherActivityInfo launcherActivityInfo = getLauncherActivityInfo(context, packageName);
            if (launcherActivityInfo != null) {
                return launcherActivityInfo.getIcon(0);
            }

            return null;
        }
    }

    @Nullable
    public static String getApplicationLabel(final Context context, final String packageName) {
        final PackageManager pm = context.getPackageManager();
        try {
            return pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString();
        } catch (final PackageManager.NameNotFoundException ignored) {
            LOG.warn("Failed to find application label for {}, attempting fallback", packageName);
            final LauncherActivityInfo launcherActivityInfo = getLauncherActivityInfo(context, packageName);
            if (launcherActivityInfo != null) {
                return launcherActivityInfo.getLabel().toString();
            }

            return null;
        }
    }

    /**
     * Fallback method to get an app info - iterate through all the users and attempt to find the
     * app. This includes work profiles.
     */
    @Nullable
    private static LauncherActivityInfo getLauncherActivityInfo(final Context context, final String packageName) {
        try {
            final LauncherApps launcherAppsService = (LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);
            final UserManager userManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
            final List<UserHandle> userProfiles = userManager.getUserProfiles();
            // Skip user 0 (current, main user)
            for (int i = 1; i < userProfiles.size(); i++) {
                final UserHandle userHandle = userProfiles.get(i);
                final List<LauncherActivityInfo> activityList = launcherAppsService.getActivityList(packageName, userHandle);

                if (!activityList.isEmpty()) {
                    LOG.debug("Found {} launcher activity infos for {} in user {}", activityList.size(), packageName, userHandle);
                    return activityList.get(0);
                }
            }

            LOG.warn("Failed to find launcher activity info for {}", packageName);
        } catch (final Exception e) {
            LOG.error("Error during launcher activity info search", e);
        }

        return null;
    }

    /**
     * Returns all applications on the device, including applications in work profiles.
     */
    @NonNull
    public static List<String> getAllApplications(final Context context) {
        final PackageManager pm = context.getPackageManager();
        final List<String> ret = new LinkedList<>();

        // Get apps for the current user
        final List<ApplicationInfo> currentUserApps = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        for (final ApplicationInfo app : currentUserApps) {
            ret.add(app.packageName);
        }

        // Add all apps from other users (eg. manager profile)
        try {
            final UserHandle currentUser = Process.myUserHandle();
            final LauncherApps launcher = (LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);
            final UserManager um = (UserManager) context.getSystemService(Context.USER_SERVICE);
            final List<UserHandle> userProfiles = um.getUserProfiles();
            for (final UserHandle userProfile : userProfiles) {
                if (userProfile.equals(currentUser)) {
                    continue;
                }

                final List<LauncherActivityInfo> userActivityList = launcher.getActivityList(null, userProfile);

                for (final LauncherActivityInfo app : userActivityList) {
                    final String packageName = app.getApplicationInfo().packageName;
                    if (!ret.contains(packageName)) {
                        ret.add(packageName);
                    }
                }
            }
        } catch (final Exception e) {
            LOG.error("Failed to get apps from other users", e);
        }

        return ret;
    }

}
