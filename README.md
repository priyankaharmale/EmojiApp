Makemoji SDK
====================

![](http://i.imgur.com/L10Uj9l.png)

**Makemoji** is a free emoji keyboard for mobile apps.

By installing our keyboard SDK every user of your app will instantly have access to new and trending emojis. Our goal is to increase user engagement as well as provide actionable real time data on sentiment (how users feel) and affinity (what users like). With this extensive data collection your per-user & company valuation will increase along with your user-base.

**Features Include**

* Extensive library of free emoji
* 722 standard Unicode emoji
* Makemoji *Flashtag* inline search system
* New emoji load dynamically and does not require a app update
* Analytics Dashboard & CMS

To obtain your SDK key please visit: http://makemoji.com

**[Learn More](http://makemoji.com)**


Library Setup
---------------------
	* minSdkVersion 15
	* targetSDKVersion 25

* Add this to your projects build.gradle file

```
dependencies {

	compile 'com.makemoji:makemoji-sdk-android:0.9.822'

}
repositories {
    jcenter()
    maven {
        url "https://dl.bintray.com/mm/maven/"
    }
}
```


SDK Usage
---------------------

**Initialization**

To start using the MakemojiSDK you will first have to add a few lines to your Application class.

If you haven't already, declare an application class in your AndroidManifest.xml using it's fully qualified class name.

```xml
    <application
        android:name="com.makemoji.sbaar.mojilist.App"
        ...
```
Then in App.java onCreate, setup your SDK key. Use the [Google Advertising ID](http://developer.android.com/google/play-services/id.html#example) if you're using Makemoji for advertising and distributing to the Play Store.

```java
    public void onCreate(){
        super.onCreate();
        Moji.initialize(this,"YOUR_KEY_HERE");
        //Moji.setUserId("Google ad id here if needed"); // optional custom user id for analytics
    }

```


**Setup the Makemoji TextInput**

First you will want to add the component to your activity content layout. Check out [styles.xml](MojiList/src/main/res/values/styles.xml) for documentation on how to theme this view.

```xml
<com.makemoji.mojilib.MojiInputLayout
       android:id="@+id/mojiInput"
       android:layout_height="wrap_content"
       android:layout_width="match_parent"
       />

```

In your activity during onCreate, find the view you added in your layout, and have it handle the back button when it is expanded.

```java
    MojiInputLayout mojiInputLayout;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mojiInputLayout = (MojiInputLayout)findViewById(R.id.mojiInput);
    }

    @Override
    public void onBackPressed(){
        if (mojiInputLayout.canHandleBack()){
            mojiInputLayout.onBackPressed();
            return;
        }
        super.onBackPressed();
    }

    @Override
        public void onMultiWindowModeChanged(boolean isInMultiWindowMode){
            super.onMultiWindowModeChanged(isInMultiWindowMode);
            mojiInputLayout.onMultiWindowModeChanged(isInMultiWindowMode);
        }
```

**Send a Message**

The SendLayoutClickListener will be called when a user clicks on the send button in the text input. This should be set in onCreate.

```java
        mojiInputLayout.setSendLayoutClickListener(new MojiInputLayout.SendClickListener() {
            @Override
            public boolean onClick(String html, Spanned spanned) {
                // send html to backend, display message
                return true;//true to clear the field, false to keep it
            }
        });

```

**Camera Button**

The CameraButtonClickListener is called when a user taps on the camera button in the text input

```java
        mojiInputLayout.setCameraButtonClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // handle camera click
            }
        });

```


**Hypermoji - Emoji with a URL**


To handle the display of a webpage when tapping on a Hypermoji ( an emoji with a URL link), use the HyperMojiClickListener callback.

```java
        mojiInputLayout.setHyperMojiClickListener(new HyperMojiListener() {
            @Override
            public void onClick(String url) {
				// handle opening URL
            }
        });

```

**Displaying Messages**


There are two key methods to use when displaying messages that are necessary for animation and Hypermoji click behavior.

Moji.setText(String html,TextView tv, boolean simple) parses the HTML and places it into a TextView, beginning any Hypermoji/gif animation of the new text and ending animation of any existing text. The "simple" argument ignores any other attributes like size or font. It should usually be set to true.
See the plain text section below for instructions if not using HTML.

Set a global HyperMojiClickListener using Moji.setDefaultHyperMojiListener(...), or one on each TextView you use, like so
```java
            textView.setTag(R.id._makemoji_hypermoji_listener_tag_id, new HyperMojiListener() {
                @Override
                public void onClick(String url) {
                    Toast.makeText(getContext(),"hypermoji clicked from adapter url " + url,Toast.LENGTH_SHORT).show();
                }
            });
```

You can also use the parseHtml method to cache the results of parsing html for ListView performance, like in [MAdapter](MojiList/src/main/java/com/makemoji/sbaar/mojilist/MAdapter.java)

**Detatched Input**

To hide the built in Edit Text and use your own that is somewhere else on the screen as the input target, call attatchMojiEditText(MojiEditText outsideMojiEdit) on the MojiInputLayout. Call detachMojiEditText() to restore the default behavior. Note that attatchMojiEditText requires an EditText that is an instance of MojiEditText to ensure keyboard compatibility and correct copy paste functionality.
```java
            mojiInputLayout.attatchMojiEditText(outsideMojiEdit);
            outsideMojiEdit.setVisibility(View.VISIBLE);
            outsideMojiEdit.requestFocus();
            ...
            String html = Moji.toHtml(outsideMojiEdit.getText());//to get input as html
            mojiInputLayout.manualSaveInputToRecentsAndBackend();//to send message to analytics console and emojis to user's recent list
```

**Plain Text Converter**

If you need to convert an html message to a platform that does not support Makemoji, you can use the plain text converter to produce a message with a human-friendly reading that converts emojis into the form [flashtagname.base62emojiid hypermojiurl]. For example, "Aliens are real [alien.gG iwanttobelieve.com]." It is recommended to store the html of the message as the canonical version, but you can also convert from plain text back to html if needed.
Note the plain text format contains no sizing information unlike html. Therefore, if you are using the "large emoji sizing" that modifies the size of the first three emojis
you will need to set a special text watcher as a tag on the textview to replicate this behavior. Make sure to null this tag if you call Moji.setText(...) with html later.

```java
        mojiInputLayout.setSendLayoutClickListener(new MojiInputLayout.SendClickListener() {
            @Override
            public boolean onClick(String html, Spanned spanned) {
                    String plainText = Moji.htmlToPlainText(html);
                    Spanned spanned = Moji.plainTextToSpanned(plainText);
                    textview.setTag(R.id._makemoji_text_watcher, Moji.getDefaultTextWatcher());
                    Moji.setText(spanned,textview);
                    }});
```

**(Optional) Emoji Wall Selection Activity**

The Emoji Wall is an activity that allows your users to select one emoji from the Makemoji library or the built in Android emoji. Declare it in your manifest to launch it. Alternatively, you can host the MojiWallFragment in your own activity that implements IMojiSelected.
```xml
        <activity
            android:name="com.makemoji.mojilib.wall.MojiWallActivity"
            android:label="Emoji Wall Activity">
        </activity>
```
```java
            Intent intent = new Intent(this, MojiWallActivity.class);
            //intent.putExtra(MojiWallActivity.EXTRA_THEME,R.style.MojiWallDefaultStyle_Light); //to theme it
            intent.putExtra(MojiWallActivity.EXTRA_SHOWRECENT,true);//show recently used emojis as a tab
            intent.putExtra(MojiWallActivity.EXTRA_SHOWUNICODE,true);//show unicode emojis as a tab
            startActivityForResult(intent,IMojiSelected.REQUEST_MOJI_MODEL);
```
The result is returned as a json string that can be converted into a MojiModel containing the name, image url, and unicode character, if applicable.
```java
     protected void onActivityResult(int requestCode, int resultCode, Intent data) {
         super.onActivityResult(requestCode,resultCode,data);
         if (requestCode == IMojiSelected.REQUEST_MOJI_MODEL && resultCode== RESULT_OK){
             try{
                 String json = data.getStringExtra(Moji.EXTRA_JSON);
                 MojiModel model = MojiModel.fromJson(new JSONObject(json));
             }
             catch (Exception e){
                 e.printStackTrace();
             }
         }
     }
```
To theme the activity, pass the activity theme as an extra when starting the activity. Make sure it extends from AppCompat and includes the following attributes.
```xml
        <item name="_mm_wall_tab_layout">@layout/mm_wall_tab</item>
        <item name="_mm_wall_header_layout">@layout/mm_wall_header</item>
        <item name="_mm_wall_tabs_bg">@android:color/black</item>
        <item name="_mm_wall_pager_bg">@color/_mm_grey_900</item>
```
**(Optional) Include the Third Party Keyboard IME!**

You can package the Makemoji keyboard in your app so users can select it as a soft keyboard no matter what app they're in. Selecting an emoji here will cause the keyboard to launch a picture share intent to the current app, or copy the image url to the clipboard if there is no matching intent filter in the current app's manifest.
Add the third party keyboard to your dependencies.
```
compile 'com.makemoji:makemoji-3pk-android:0.9.822'
```
In strings.xml, set the provider authority for the keyboards' content provider based on your unique package name, add the keyboard name as it will appear to the user and the class name of the keyboard's settings activity. Make sure to prompt the user to activate the keyboard after installation using code similar to ActivateActivity, or the keyboard won't show up as an option.
**If you are publishing multiple apps, each provider authority must be unique**  or there will be installation problems!
```xml
    <!-- strings.xml -->
    <string name="_mm_provider_authority">com.makemoji.keyboard.fileprovider</string>
    <string name="_mm_kb_label">MakeMoji Keyboard (Sample App)</string>
    <string name="_mm_kb_share_message">MakeMoji Keyboard (Sample App)</string> <!-- share button invisible if empty -->
    <string name="_mm_kb_settings_activity">com.makemoji.sbaar.mojilist.ActivateActivity</string>
```
```xml
    <!-- optional customization -->
    <color name="mmKBPageTitleColor">@color/colorPrimary</color>
    <color name="mmKBIconColor">@colom r/colorPrimary</color>
    <color name="mmKBProgressColor">@color/_mm_default_send_text_color</color>

    <integer name="mm_3pk_rows">5</integer>
    <integer name="mm_3pk_cols">8</integer>
    <integer name="mm_3pk_gif_rows">2</integer>

    <dimen name="mmKBMaxTabWidth">38dp</dimen>
    <dimen name="mmKBMaxTabSize">36dp</dimen>

    <!--Reuse bitmaps used for spans in textview; turn this off when using fewer rows/cols in 3pk -->
    <bool name="mmUseSpanSizeFor3pkImages">false</bool>
    <!-- replace kb_tab_bg.xml with a selector to change the keyboard tab background -->
```
If you want the ability to customize some other aspect of the sdk or 3pk, just ask.

**(Optional) Control Locked Categories**

If you choose to lock some of your categories, you can control which are unlocked for the device by using the MojiUnlock class. To listen for when a user clicks a locked category in the MojiInpuLayout, set a listener.
```java
        mojiInputLayout.setLockedCategoryClicked(new MojiUnlock.ILockedCategoryClicked() {
            @Override
            public void onClick(String name) {
                MojiUnlock.unlockCategory(name);
                mojiInputLayout.refreshCategories();
            }
        });
```

If using the 3pk, you can either set a global listener for when a keybaord category is selected, or listen to the default intent fired when a locked category is clicked.
Add an intent filter to activity to be launched for the action "com.makemoji.mojilib.action.LOCKED_CATEGORY_CLICKED".

```java
        MMKB.setCategoryListener(new MMKB.ICategorySelected() {
            View v;
            @Override
            public void categorySelected(String category,boolean locked, final FrameLayout parent) {
            ...
            }});
            //OR in an activity
            
            public void onNewIntent(Intent i){
                super.onNewIntent(i);
                if (Moji.ACTION_LOCKED_CATEGORY_CLICK.equals(i.getAction())){
                    lockedCategoryClick(i.getStringExtra(Moji.EXTRA_CATEGORY_NAME));
                }
            }
```

To customize the image overlaid onto a locked category icon, add mm_locked_foreground.xml to your drawable folder. It can look something like this
```xml
    <?xml version="1.0" encoding="utf-8"?>
    <bitmap  xmlns:android="http://schemas.android.com/apk/res/android"  android:src="@drawable/mm_placeholder"  android:alpha=".8" />
```

If you are using the Emoji Wall, extend MojiWallActivity to respond to a locked category being selected and launch this activity instead.
```java
    public class MyMojiWallActivity extends MojiWallActivity{
    ...
        @Override
        public void lockedCategoryClick(String name) {
            MojiUnlock.addGroup(name);//unlock the category
            fragment.refresh();//refresh the tabs
        }
    }
```

**(Optional) Emoji reactions**

To include a horizontal view of emoji reactions for a piece of content, include a ReactionsLayout with a given height, like 30dp.
```xml
    <com.makemoji.mojilib.ReactionsLayout
        android:id="@+id/reactions_layout"
        android:layout_width="match_parent"
        android:layout_height="30dp"/>
```
You must set an id corresponding to a unique piece of your content on each reaction layout, using reactionsLayout.setReactionsData(new ReactionData(id)); Make sure to hold onto this data object if you wish to assign it to a view again, such as in a list adapter.

Each ReactionsLayout has an 'add' button that will open the MojiWallActivity for the user to select one, so make sure to add it to your manifest as described above. To update the ReactionsLayout with the user selection, add the following in your onActivityResult.
```java
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        ReactionsData.onActivityResult(requestCode,resultCode,data);
    }
```

**(Optional) Offline Mode**

To package emojis with your app and have no online dependencies, run this [script](https://github.com/makemoji/MakemojiSDKCocoapod/blob/8e4f347328be4bb6e8799f9bdf17e2d999747b05/Pod/downloadEmoji.php)
and place the output under res/assets/makemoji Then run the following code after initialisation.
```java
    Moji.setEnableUpdates(false);//offline mode
    //call only first time or when new assets are in the app after an update
    Moji.loadOfflineFromAssets();
```
**Proguard Setup**

If you need to use proguard when signing your app, add the following to your proguard-rules.pro file
```
-keep class com.makemoji.** { *; }

-dontwarn okio.**
-dontwarn retrofit2.**
-dontwarn rx.**

-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

-keepattributes Signature
-keepattributes Exceptions

-dontwarn android.app.Activity #If target sdk <24

```


FAQ
---------------------

*	The Makemoji SDK is completely free.

*	All emojis are served from AWS S3.

*	We do not store your messages. Your app backend will have to process and serve messages created with our SDK.

*	We do not send push notifications.

*	Your app's message volume does not affect the performance of our SDK.

*	Messages are composed of simple HTML containing image and paragraph tags. Formatting is presented as inline CSS.

*  All network operations happen asynchronously and do not block the User Interface

Service Performance
---------------------

* Avg Service Repsonse Time: 100ms

* Hosted with AWS using Elastic Beanstalk & RDS

* Scales seamlessly to meet traffic demands
