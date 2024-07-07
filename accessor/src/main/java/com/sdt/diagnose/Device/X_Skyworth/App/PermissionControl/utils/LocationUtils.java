/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.sdt.diagnose.Device.X_Skyworth.App.PermissionControl.utils;

import android.Manifest;
import android.content.Context;
import android.location.LocationManager;

import androidx.annotation.NonNull;

public class LocationUtils {
    public static final String LOCATION_PERMISSION = Manifest.permission_group.LOCATION;

    public static boolean isLocationEnabled(Context context) {
        return context.getSystemService(LocationManager.class).isLocationEnabled();
    }

    public static boolean isLocationGroupAndProvider(Context context, String groupName,
                                                     String packageName) {
        return LOCATION_PERMISSION.equals(groupName)
                && context.getSystemService(LocationManager.class).isProviderPackage(packageName);
    }

    public static boolean isLocationGroupAndControllerExtraPackage(@NonNull Context context,
                                                                   @NonNull String groupName, @NonNull String packageName) {
        return LOCATION_PERMISSION.equals(groupName)
                && packageName.equals(context.getSystemService(LocationManager.class)
                .getExtraLocationControllerPackage());
    }

    /**
     * Returns whether the location controller extra package is enabled.
     */
    public static boolean isExtraLocationControllerPackageEnabled(Context context) {
        try {
            return context.getSystemService(LocationManager.class)
                    .isExtraLocationControllerPackageEnabled();
        } catch (Exception e) {
            return false;
        }

    }
}
