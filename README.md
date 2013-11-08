Android Sliding Up Panel
=========================

The 2.2 version of the [Umano](http://umanoapp.com) [Android app](https://play.google.com/store/apps/details?id=com.sothree.umano) features a sexy sliding up draggable panel for the currently playing article. This type of a panel is a common pattern also used in the Google Music app and the Rdio app. This is an open source implementation of this component that you are free to take advantage of in your apps. Umano Team <3 Open Source.

As seen in Umano ([http://umanoapp.com](http://umanoapp.com)):

![SlidingUpPanelLayout](https://raw.github.com/umano/AndroidSlidingUpPanelDemo/master/slidinguppanel.png)

### Usage

To use the layout, simply include `com.sothree.slidinguppaneldemo.SlidingUpPanelLayout` as the Root element in your activity Layout. Make sure that it has two children. The first child is your main layout. The second child is your layout for the sliding up panel. Both children should have width and height set to `match_parent`. For more information, please refer to the sample code.
```xml
    <com.sothree.slidinguppanel.SlidingUpPanelLayout
        android:id="@+id/sliding_layout"
        android:layout_width="match_parent"
        android:layout_height="match_parent" >

        <TextView
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:gravity="center"
            android:text="Main Content"
            android:textSize="16sp" />

        <TextView
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:gravity="center|top"
            android:text="The Awesome Sliding Up Panel"
            android:textSize="16sp" />
    </com.sothree.slidinguppanel.SlidingUpPanelLayout>
```
For smooth interaction with the ActionBar, make sure that `windowActionBarOverlay` is set to `true` in your styles:
```xml
<style name="AppTheme">
    <item name="android:windowActionBarOverlay">true</item>
</style>
```
### Additional Features

You can restrict the drag area of the sliding panel to a specific view by using the `setDragView` method. Otherwise, the whole panel will be slideable and it will intercept all clicks.

You can change the panel height by using the `setPanelHeight` method.

You can change the panel shadow by using the `setShadowDrawable` method. No shadow is displayed by default.

You can disable sliding by using `setSlidingEnabled` method. This can be useful if you want to show\hide panel only from the code. By default sliding is enabled.

You can set a anchor point in the middle of the screen using `setAnchorPoint` to allow an intermediate expanded state for the panel (similar to Google Maps).

### Implementation

This code is heavily based on the opened-sourced [SlidingPaneLayout](http://developer.android.com/reference/android/support/v4/widget/SlidingPaneLayout.html) component from the r13 of the Android Support Library. Thanks Android team!

### Requrements

Tested on Android 2.2+

### Other Contributors 

Aug 20, 13 - ([@gipi](https://github.com/gipi)) - Android Studio Support

Jul 24, 13 - Philip Schiffer ([@hameno](https://github.com/hameno)) - Maven Support

Oct 20, 13 - Irina Preșa ([@iriina](https://github.com/iriina)) - Anchor Support

If you have an awesome pull request, send it over!

### Licence

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this work except in compliance with the License.
You may obtain a copy of the License in the LICENSE file, or at:

  [http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
