
# MyOneTimeInitializer

#### What's MyOneTimeInitializer
MyOneTimeInitializer is used to update Launcher2 database during OTA to correct invalid favorite components.

For more information, see:  
[`Update_Launcher2_database_during_OTA_to_correct_invalid_favorite_components`](https://github.com/northbright/Notes/blob/master/Android/OnetimeInitializer/Update_Launcher2_database_during_OTA_to_correct_invalid_favorite_components.md)

#### Features

  * Support GMS GoogleOneTimeInitializer by default. See `AndroidManifest.xml`:  
        
        <receiver
            android:name=".OneTimeInitializerReceiver">
            <intent-filter>
                <!-- If GMS is prebuilt, GoogleOneTimeInitializer will send this broadcast after it's done -->
                <action
                    android:name="com.google.android.onetimeinitializer.ONE_TIME_INITIALIZED"/>
                <!-- If GMS is NOT prebuilt, use receiver for BOOT_COMPLETED as AOSP's OneTimeInitializer
                <action
                    android:name="android.intent.action.BOOT_COMPLETED"/>
                -->
            </intent-filter>
        </receiver>

  * Use string array resources to input old / new components(which can be found in Sqlite Database Browser), no need to modify source code.    
    To update components in launcher.db, add two string array resources for old components and new components(same order and count):  
    `vi res/values/update_components.xml`:  

        <?xml version="1.0" encoding="utf-8"?>
        <resources>
            <string-array name="old_components" translatable="false">
                <item>com.box.android/.activities.SplashScreenActivity</item>
                <item>com.google.android.gms/.common.settings.GoogleSettingsActivity</item>
                <item>com.android.gallery3d/com.android.camera.CameraLauncher</item>
            </string-array>
            <string-array name="new_components" translatable="false">
                <item></item> <!-- leave empty means remove this component in db -->
                <item>com.google.android.gms/.app.settings.GoogleSettingsActivity</item>
                <item>com.google.android.GoogleCamera/com.android.camera.CameraLauncher</item>
            </string-array>
        </resources>

    Leave empty`<item></item>` means you want to remove such favorite in launcher.db