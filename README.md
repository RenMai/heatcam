# Nokia_project_ThermalCamera

## Project description
Android application to measure user's temperature and indicate for higher than normal temperature.<br/>
Application produced in cooperation with LeViteZer and Metropolia University of Applied Sciences.<br/>

## Development environment
Created using Android Studio (v. 4.1.1).<br/>

## Dependencies
For all dependencies, check build.gradle file in heatcam/app folder.

### Main dependencies
Face detection uses Google's MLKit (https://developers.google.com/ml-kit/vision/face-detection/android)
```
implementation 'com.google.mlkit:face-detection:16.0.2'
```
Camera api used is CameraX api (https://developer.android.com/training/camerax)
```
implementation "androidx.camera:camera-camera2:1.0.0-beta05"
implementation "androidx.camera:camera-lifecycle:1.0.0-beta05"
implementation "androidx.camera:camera-view:1.0.0-alpha12"
implementation "androidx.camera:camera-extensions:1.0.0-alpha12"
```
USB serial library by mik3y (https://github.com/mik3y/usb-serial-for-android)
```
implementation 'com.github.mik3y:usb-serial-for-android:v3.0.1'
```
MPAndroidChart library used for displaying charts by PhilJay (https://github.com/PhilJay/MPAndroidChart)
```
implementation 'com.github.PhilJay:MPAndroidChart:v3.1.0'
```

