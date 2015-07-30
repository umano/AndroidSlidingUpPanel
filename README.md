[![JitPack](https://img.shields.io/github/release/TR4Android/AndroidSlidingUpPanel.svg?label=JitPack)]()

Android Sliding Up Panel - Material Design
===========================================
This is a fork of Umano Sliding Up Panel that aims to bring some Material Design features:

#### Floating Action Button

Added the ability to attach a Floating Action Button to the Sliding Up Panel (as seen in the Google Maps Material Design version). To include the Floating Action Button to your layout change it to this:
```xml
<com.sothree.slidinguppanel.FloatingActionButtonLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:sothree="http://schemas.android.com/apk/res-auto"
    xmlns:fab="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    sothree:umanoFabMode=["leave_behind" | "circular_reveal" | "fade"]
    tools:context=".DemoActivity">
    
    <!-- SLIDING UP PANEL -->
    <com.sothree.slidinguppanel.SlidingUpPanelLayout
        android:id="@+id/sliding_layout"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:gravity="bottom"
        sothree:umanoDragView="@+id/dragView"
        sothree:umanoPanelHeight="68dp"
        sothree:umanoParalaxOffset="100dp"
        sothree:umanoShadowHeight="4dp">
        <!-- The normal content of the Sliding Up Panel (see Original Readme)-->
    </com.sothree.slidinguppanel.SlidingUpPanelLayout>

    <!-- FLOATING ACTION BUTTON -->
    <com.melnykov.fab.FloatingActionButton
        android:id="@+id/fab"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_gravity="bottom|right"
        android:layout_marginRight="16dp"
        android:layout_marginBottom="16dp"
        android:src="@android:drawable/ic_input_add"
        fab:fab_colorNormal="@color/primary"
        fab:fab_colorPressed="@color/primary_pressed"
        fab:fab_colorRipple="@color/ripple" />
</com.sothree.slidinguppanel.FloatingActionButtonLayout>
```
(The Floating Action Button used here and in the demo is [Oleksandr Melnykov's Floating Action Button](https://github.com/makovkastar/FloatingActionButton))

You can choose the kind of animation you want for the Floating Action Button when dragging the panel by using the `umanoFabMode` attribute:
* `leave_behind`: This gradually moves the Floating Action Button from the top of the panel header in the collapsed state to the bottom of the header in the expanded state.
* `circular_reveal`: This keeps the Floating Action button on top of the panel header and show or hides the Floating Action Button based on a threshold value of how far the panel has been dragged to the top using a circular reveal animation (see the Google Maps Floating Action Button). Thanks to [@flyingtoaster0](https://github.com/flyingtoaster0) for contributing code!
* `fade`: This animates the alpha value of the Floating Action Button based on how far the panel has been dragged to the top.

There also are some new methods related to the Floating Action Button:
* `setFloatingActionButtonVisibility(int visibility)`: This is a replacment method for the standard `setVisibility()` which doesn't work as intended as this library handles the visibility while sliding the panel. It takes the normal `View.VISIBLE`, `View.INVISIBLE` or `View.GONE` as input. Use this one instead of the default one whenever you want to change the visibility of the Floating Action Button.
* `setFloatingActionButtonAttached(boolean attached)`: This can be used to attach or detach the Floating Action Button from the sliding up panel. When `attached` is `true` the library will move the Floating Action Button along, when it is `false` the Floating Action Button will remain at its' position. **Note:** it is currently your responsibility that the transition from detached to attached mode doesn't result in a position jump.

#### New Listeners
Added new listeners. Here's a list of the new ones along with a explanation:
* `onPanelCollapsedStateY(View panel, boolean reached)`: This gets called whenever the user reaches or leaves the collapsed state, even while dragging. If boolean reached is true, the panel has just reached the collapsed postion, if it is false, it has just left the collapsed position.
* `onPanelExpandedStateY(View panel, boolean reached)`: This gets called whenever the user reaches or leaves the expanded state, even while dragging. If boolean reached is true, the panel has just reached the expanded postion, if it is false, it has just left the expanded position.
* `onPanelLayout(View panel, PanelState state)`: This gets called whenever the Sliding Up Panel gets laid out freshly. This happens especially when the screen orientation or size is changed. It can be used to apply changes that should have been done via `onPanelCollapsedStateY(View panel, boolean reached)` or `onPanelExpandedStateY(View panel, boolean reached)` but have been lost due to orientation or size change. This probably won't be needed when `android:configChanges="orientation|screenSize"` is set in your Manifest.
* `onPanelHiddenExecuted(View panel, Interpolator interpolator, int duration)`: This gets called whenever the application calls `setPanelState(PanelState.HIDDEN)` and the Sliding Panel isn't yet hidden. It provides interpolator and duration for any animated changes that could be made.
* `onPanelShownExecuted(View panel, Interpolator interpolator, int duration)`: This gets called whenever the application calls `setPanelState(PanelState.COLLAPSED)` and the Sliding Panel isn't yet shown. It provides interpolator and duration for any animated changes that could be made.

#### Importing the library
As this fork of the library currently is not available on Maven Central, you'll for now have to do some extra steps to include this to your project. I hope to eventually get these changes back into the main library though.

##### Download as Zip File

1. Download the repository as a [.zip file](https://github.com/TR4Android/AndroidSlidingUpPanel/archive/master.zip)
2. Unzip the .zip file you just downloaded
3. Copy the `library` folder into your project folder (you can also rename it if you have to because of conflicts)
4. Add `include ':library'` or `include ':theChangedLibraryName'` if you changed the folder name to your `settings.gradle` of your project folder
5. Add `compile project(':library')` or `compile project(':theChangedLibraryName')` if you changed the folder name to your dependencies of the `build.gradle` file of your app module
6. Now you should be able to work with this library

##### Gradle dependency

You can now use the library as a gradle dependency thanks to [JitPack](https://github.com/jitpack/jitpack.io). Just add the following to the `build.gradle` of your app module:
```gradle
repositories { 
    maven { url "https://jitpack.io" }
}
dependencies {
    compile 'com.github.TR4Android:AndroidSlidingUpPanel:3.1.2'
}
```  

Android Sliding Up Panel - Orginal Readme
==========================================

This library provides a simple way to add a draggable sliding up panel (popularized by Google Music, Google Maps and Rdio) to your Android application. Umano Team <3 Open Source.

As seen in [Umano](http://umanoapp.com) [Android app](https://play.google.com/store/apps/details?id=com.sothree.umano):

![SlidingUpPanelLayout](https://raw.github.com/umano/AndroidSlidingUpPanelDemo/master/slidinguppanel.png)

### Importing the library

#### Eclipse 

Download the [latest release](https://github.com/umano/AndroidSlidingUpPanel/releases) and include the `library` project as a dependency in Eclipse.

#### Android Studio 

Simply add the following dependency to your `build.gradle` file to use the latest version:

```groovy
dependencies {
    repositories {
        mavenCentral()
    }
    compile 'com.sothree.slidinguppanel:library:3.1.1'
}
```

### Usage 

* Include `com.sothree.slidinguppanel.SlidingUpPanelLayout` as the root element in your activity layout.
* The layout must have `gravity` set to either `top` or `bottom`.
* Make sure that it has two children. The first child is your main layout. The second child is your layout for the sliding up panel.
* The main layout should have the width and the height set to `match_parent`.
* The sliding layout should have the width set to `match_parent` and the height set to either `match_parent`, `wrap_content` or the max desireable height.
* By default, the whole panel will act as a drag region and will intercept clicks and drag events. You can restrict the drag area to a specific view by using the `setDragView` method or `umanoDragView` attribute. 

For more information, please refer to the sample code.

```xml
<com.sothree.slidinguppanel.SlidingUpPanelLayout
    xmlns:sothree="http://schemas.android.com/apk/res-auto"
    android:id="@+id/sliding_layout"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:gravity="bottom"
    sothree:umanoPanelHeight="68dp"
    sothree:umanoShadowHeight="4dp">

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
However, in this case you would likely want to add a top margin to your main layout of `?android:attr/actionBarSize`
or `?attr/actionBarSize` to support older API versions.

### Caveats, Additional Features and Customization

* If you are using a custom `umanoDragView`, the panel will pass through the click events to the main layout. Make your second layout `clickable` to prevent this.
* You can change the panel height by using the `setPanelHeight` method or `umanoPanelHeight` attribute.
* If you would like to hide the shadow above the sliding panel, set `shadowHeight` attribute to 0.
* Use `setEnabled(false)` to completely disable the sliding panel (including touch and programmatic sliding)
* Use `setTouchEnabled(false)` to disables panel's touch responsiveness (drag and click), you can still control the panel programatically
* Use `getPanelState` to get the current panel state
* Use `setPanelState` to set the current panel state
* You can add paralax to the main view by setting `umanoParalaxOffset` attribute (see demo for the example).
* You can set a anchor point in the middle of the screen using `setAnchorPoint` to allow an intermediate expanded state for the panel (similar to Google Maps).
* You can set a `PanelSlideListener` to monitor events about sliding panes.
* You can also make the panel slide from the top by changing the `layout_gravity` attribute of the layout to `top`.
* If you have a ScrollView or a ListView inside of the panel, make sure to set `umanoScrollableView` attribute on the panel to supported nested scrolling.
* By default, the panel pushes up the main content. You can make it overlay the main content by using `setOverlayed` method or `umanoOverlay` attribute. This is useful if you would like to make the sliding layout semi-transparent. You can also set `umanoClipPanel` to false to make the panel transparent in non-overlay mode.
* By default, the main content is dimmed as the panel slides up. You can change the dim color by changing `umanoFadeColor`. Set it to `"@android:color/transparent"` to remove dimming completely.

### Implementation

This library was initially based on the opened-sourced [SlidingPaneLayout](http://developer.android.com/reference/android/support/v4/widget/SlidingPaneLayout.html) component from the r13 of the Android Support Library. Thanks Android team!

### Requirements

Tested on Android 2.2+

### Other Contributors

* Jan 21, 14 - ChaYoung You ([@yous](https://github.com/yous)) - Slide from the top support
* Aug 20, 13 - ([@gipi](https://github.com/gipi)) - Android Studio Support
* Jul 24, 13 - Philip Schiffer ([@hameno](https://github.com/hameno)) - Maven Support
* Oct 20, 13 - Irina Preșa ([@iriina](https://github.com/iriina)) - Anchor Support
* Dec 1, 13 - ([@youchy](https://github.com/youchy)) - XML Attributes Support
* Dec 22, 13 - Vladimir Mironov ([@MironovNsk](https://github.com/nsk-mironov)) - Custom Expanded Panel Height

If you have an awesome pull request, send it over!

### Changelog

* 3.1.0
  * Added `umanoScrollableView` to supported nested scrolling in children (only ScrollView and ListView are supported for now)
* 3.0.0
  * Added `umano` prefix for all attributes
  * Added `clipPanel` attribute for supporting transparent panels in non-overlay mode.
  * `setEnabled(false)` - now completely disables the sliding panel (touch and programmatic sliding)
  * `setTouchEnabled(false)` - disables panel's touch responsiveness (drag and click), you can still control the panel programatically
  * `getPanelState` - is now the only method to get the current panel state
  * `setPanelState` - is now the only method to modify the panel state from code
* 2.0.2 - Allow `wrap_content` for sliding view height attribute. Bug fixes. 
* 2.0.1 - Bug fixes. 
* 2.0.0 - Cleaned up various public method calls. Added animated `showPanel`/`hidePanel` methods. 
* 1.0.1 - Initial Release 

### Licence

> Licensed under the Apache License, Version 2.0 (the "License");
> you may not use this work except in compliance with the License.
> You may obtain a copy of the License in the LICENSE file, or at:
>
>  [http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)
>
> Unless required by applicable law or agreed to in writing, software
> distributed under the License is distributed on an "AS IS" BASIS,
> WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
> See the License for the specific language governing permissions and
> limitations under the License.
