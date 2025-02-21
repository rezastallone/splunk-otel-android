/*
 * Copyright Splunk Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.splunk.rum;

import android.annotation.SuppressLint;
import android.app.Application;
import android.os.Build;
import java.lang.reflect.Method;
import java.util.Objects;

final class BackgroundProcessDetector {

    private BackgroundProcessDetector() {}

    static Boolean isBackgroundProcess(String applicationId) {
        String applicationProcessName = getApplicationProcessName();
        // If application Id is same as application processName, the app is visible to user.
        // Using inverted condition to determine background processes.
        return !Objects.equals(applicationProcessName, applicationId);
    }

    private static String getApplicationProcessName() {
        if (Build.VERSION.SDK_INT >= 28) {
            return Application.getProcessName();
        }
        try {
            @SuppressLint("PrivateApi")
            Class<?> activityThread = Class.forName("android.app.ActivityThread");
            String methodName = "currentProcessName";
            @SuppressLint("PrivateApi")
            Method getProcessName = activityThread.getDeclaredMethod(methodName);
            return (String) getProcessName.invoke(null);
        } catch (Exception e) {
            return "";
        }
    }
}
