# capacitor-wechat
<a href="https://capgo.app/"><img src="https://capgo.app/readme-banner.svg?repo=Cap-go/capacitor-wechat" alt="Capgo - Instant updates for Capacitor" /></a>

<div align="center">
  <h2><a href="https://capgo.app/?ref=plugin_wechat"> ➡️ Get Instant updates for your App with Capgo</a></h2>
  <h2><a href="https://capgo.app/consulting/?ref=plugin_wechat"> Missing a feature? We'll build the plugin for you 💪</a></h2>
</div>

WeChat SDK for Capacitor - enables authentication, sharing, payments, and mini-programs.

## Documentation

The most complete doc is available here: https://capgo.app/docs/plugins/wechat/

## Compatibility

| Plugin version | Capacitor compatibility | Maintained |
| -------------- | ----------------------- | ---------- |
| v8.\*.\*       | v8.\*.\*                | ✅          |
| v7.\*.\*       | v7.\*.\*                | On demand   |
| v6.\*.\*       | v6.\*.\*                | ❌          |
| v5.\*.\*       | v5.\*.\*                | ❌          |

> **Note:** The major version of this plugin follows the major version of Capacitor. Use the version that matches your Capacitor installation (e.g., plugin v8 for Capacitor 8). Only the latest major version is actively maintained.

## Install

You can use our AI-Assisted Setup to install the plugin. Add the Capgo skills to your AI tool using the following command:

```bash
npx skills add https://github.com/cap-go/capacitor-skills --skill capacitor-plugins
```

Then use the following prompt:

```text
Use the `capacitor-plugins` skill from `cap-go/capacitor-skills` to install the `@capgo/capacitor-wechat` plugin in my project.
```

If you prefer Manual Setup, install the plugin by running the following commands and follow the platform-specific instructions below:

```bash
npm install @capgo/capacitor-wechat
npx cap sync
```

## Configuration

### Capacitor config

Add your WeChat credentials to `capacitor.config.ts` (or `.json`). You can override them at runtime by calling `CapacitorWechat.initialize(...)`.

```ts
const config: CapacitorConfig = {
  appId: 'com.example.app',
  appName: 'Example',
  webDir: 'dist',
  plugins: {
    CapacitorWechat: {
      appId: 'wx1234567890abcdef',
      universalLink: 'https://your-universal-link.example.com/'
    }
  }
};
```

### iOS

Add the following to your `Info.plist`:

```xml
<key>LSApplicationQueriesSchemes</key>
<array>
  <string>weixin</string>
  <string>weixinULAPI</string>
</array>

<key>CFBundleURLTypes</key>
<array>
  <dict>
    <key>CFBundleTypeRole</key>
    <string>Editor</string>
    <key>CFBundleURLName</key>
    <string>weixin</string>
    <key>CFBundleURLSchemes</key>
    <array>
      <string>YOUR_WECHAT_APP_ID</string>
    </array>
  </dict>
</array>
```

You'll need to integrate the WeChat SDK into your iOS project. Add the WeChat SDK to your `Podfile` or download it from the [WeChat Open Platform](https://developers.weixin.qq.com/doc/oplatform/en/Mobile_App/Access_Guide/iOS.html).

**Important:** iOS requires a valid Universal Link to be configured. You must set the `universalLink` parameter either in your `capacitor.config` (as shown above) or when calling `initialize()`. Without a valid Universal Link, the app will crash during WeChat SDK initialization. Learn more about configuring Universal Links in the [WeChat iOS documentation](https://developers.weixin.qq.com/doc/oplatform/en/Mobile_App/Access_Guide/iOS.html).

### Android

Add the following to your `AndroidManifest.xml`:

```xml
<manifest>
  <application>
    <!-- WeChat callback activity -->
    <activity
      android:name=".wxapi.WXEntryActivity"
      android:exported="true"
      android:label="@string/app_name"
      android:launchMode="singleTask"
      android:theme="@android:style/Theme.Translucent.NoTitleBar">
      <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
      </intent-filter>
    </activity>
  </application>
</manifest>
```

You'll need to integrate the WeChat SDK into your Android project. Add the WeChat SDK dependency to your `build.gradle` or download it from the [WeChat Open Platform](https://developers.weixin.qq.com/doc/oplatform/en/Mobile_App/Access_Guide/Android.html).

Create `WXEntryActivity` and `WXPayEntryActivity` inside your application's package (`your.package.name.wxapi`). Both activities should extend `ee.forgr.plugin.capacitor_wechat.WechatResponseActivity` so the plugin can receive callbacks from WeChat:

```java
package your.package.name.wxapi;

import ee.forgr.plugin.capacitor_wechat.WechatResponseActivity;

public class WXEntryActivity extends WechatResponseActivity {}

public class WXPayEntryActivity extends WechatResponseActivity {}
```

Register these activities in your app manifest just like the snippet above (WeChat requires both for general SDK + Pay callbacks).

## Setup

Before using any WeChat functionality, you need to register your app with a WeChat App ID from the [WeChat Open Platform](https://open.weixin.qq.com/). Once you have the credentials, either place them in `capacitor.config` or call `CapacitorWechat.initialize` before making other calls:

```ts
import { CapacitorWechat } from '@capgo/capacitor-wechat';

await CapacitorWechat.initialize({
  appId: 'wx1234567890abcdef',
  universalLink: 'https://your-universal-link.example.com/'
});
```

## API

<docgen-index>

* [`initialize(...)`](#initialize)
* [`isInstalled()`](#isinstalled)
* [`auth(...)`](#auth)
* [`share(...)`](#share)
* [`sendPaymentRequest(...)`](#sendpaymentrequest)
* [`openMiniProgram(...)`](#openminiprogram)
* [`chooseInvoice(...)`](#chooseinvoice)
* [`getPluginVersion()`](#getpluginversion)
* [Interfaces](#interfaces)

</docgen-index>

<docgen-api>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

Capacitor WeChat Plugin - WeChat SDK integration for authentication, sharing, payments, and mini-programs.

### initialize(...)

```typescript
initialize(options: WechatInitializationOptions) => Promise<void>
```

Initialize the WeChat SDK with your application credentials.

You can also set these values in `capacitor.config.ts` under the `CapacitorWechat`
plugin configuration. Calling this method overrides any bundled configuration at runtime.

| Param         | Type                                                                                | Description                                                   |
| ------------- | ----------------------------------------------------------------------------------- | ------------------------------------------------------------- |
| **`options`** | <code><a href="#wechatinitializationoptions">WechatInitializationOptions</a></code> | - Initialization options including the required WeChat App ID |

**Since:** 1.1.0

--------------------


### isInstalled()

```typescript
isInstalled() => Promise<{ installed: boolean; }>
```

Check if WeChat app is installed on the device.

**Returns:** <code>Promise&lt;{ installed: boolean; }&gt;</code>

**Since:** 1.0.0

--------------------


### auth(...)

```typescript
auth(options: WechatAuthOptions) => Promise<WechatAuthResponse>
```

Authenticate user with WeChat OAuth.

| Param         | Type                                                            | Description                              |
| ------------- | --------------------------------------------------------------- | ---------------------------------------- |
| **`options`** | <code><a href="#wechatauthoptions">WechatAuthOptions</a></code> | - Authentication options including scope |

**Returns:** <code>Promise&lt;<a href="#wechatauthresponse">WechatAuthResponse</a>&gt;</code>

**Since:** 1.0.0

--------------------


### share(...)

```typescript
share(options: WechatShareOptions) => Promise<void>
```

Share content to WeChat.

| Param         | Type                                                              | Description                                        |
| ------------- | ----------------------------------------------------------------- | -------------------------------------------------- |
| **`options`** | <code><a href="#wechatshareoptions">WechatShareOptions</a></code> | - Share options including type, scene, and content |

**Since:** 1.0.0

--------------------


### sendPaymentRequest(...)

```typescript
sendPaymentRequest(options: WechatPaymentOptions) => Promise<void>
```

Send payment request to WeChat Pay.

| Param         | Type                                                                  | Description                           |
| ------------- | --------------------------------------------------------------------- | ------------------------------------- |
| **`options`** | <code><a href="#wechatpaymentoptions">WechatPaymentOptions</a></code> | - Payment request options from server |

**Since:** 1.0.0

--------------------


### openMiniProgram(...)

```typescript
openMiniProgram(options: WechatMiniProgramOptions) => Promise<{ extMsg?: string; }>
```

Open WeChat mini-program.

| Param         | Type                                                                          | Description                                        |
| ------------- | ----------------------------------------------------------------------------- | -------------------------------------------------- |
| **`options`** | <code><a href="#wechatminiprogramoptions">WechatMiniProgramOptions</a></code> | - Mini-program options including username and path |

**Returns:** <code>Promise&lt;{ extMsg?: string; }&gt;</code>

**Since:** 1.0.0

--------------------


### chooseInvoice(...)

```typescript
chooseInvoice(options: WechatInvoiceOptions) => Promise<WechatInvoiceResponse>
```

Choose invoice from WeChat.

| Param         | Type                                                                  | Description                 |
| ------------- | --------------------------------------------------------------------- | --------------------------- |
| **`options`** | <code><a href="#wechatinvoiceoptions">WechatInvoiceOptions</a></code> | - Invoice selection options |

**Returns:** <code>Promise&lt;<a href="#wechatinvoiceresponse">WechatInvoiceResponse</a>&gt;</code>

**Since:** 1.0.0

--------------------


### getPluginVersion()

```typescript
getPluginVersion() => Promise<{ version: string; }>
```

Get the native Capacitor plugin version.

**Returns:** <code>Promise&lt;{ version: string; }&gt;</code>

**Since:** 1.0.0

--------------------


### Interfaces


#### WechatInitializationOptions

WeChat initialization options.

| Prop                | Type                | Description                                                         |
| ------------------- | ------------------- | ------------------------------------------------------------------- |
| **`appId`**         | <code>string</code> | Required WeChat application ID.                                     |
| **`universalLink`** | <code>string</code> | iOS universal link that is associated with your WeChat application. |


#### WechatAuthResponse

WeChat authentication response.

| Prop        | Type                | Description                                      |
| ----------- | ------------------- | ------------------------------------------------ |
| **`code`**  | <code>string</code> | Authorization code to exchange for access token. |
| **`state`** | <code>string</code> | State parameter if provided in request.          |


#### WechatAuthOptions

WeChat authentication options.

| Prop        | Type                | Description                                                                        |
| ----------- | ------------------- | ---------------------------------------------------------------------------------- |
| **`scope`** | <code>string</code> | OAuth scope. Use 'snsapi_userinfo' for user info or 'snsapi_login' for login only. |
| **`state`** | <code>string</code> | Optional state parameter for CSRF protection.                                      |


#### WechatShareOptions

WeChat share options.

| Prop                        | Type                                                                            | Description                                                                     |
| --------------------------- | ------------------------------------------------------------------------------- | ------------------------------------------------------------------------------- |
| **`scene`**                 | <code>number</code>                                                             | Share scene: 0 = Session (chat), 1 = Timeline (moments), 2 = Favorite.          |
| **`type`**                  | <code>'text' \| 'image' \| 'link' \| 'music' \| 'video' \| 'miniprogram'</code> | Share type: 'text', 'image', 'link', 'music', 'video', 'miniprogram'.           |
| **`text`**                  | <code>string</code>                                                             | Text content (for type 'text').                                                 |
| **`title`**                 | <code>string</code>                                                             | Title (for type 'link', 'music', 'video', 'miniprogram').                       |
| **`description`**           | <code>string</code>                                                             | Description (for type 'link', 'music', 'video', 'miniprogram').                 |
| **`link`**                  | <code>string</code>                                                             | Link URL (for type 'link').                                                     |
| **`imageUrl`**              | <code>string</code>                                                             | Image URL or base64 data.                                                       |
| **`thumbUrl`**              | <code>string</code>                                                             | Thumbnail URL or base64 data (for type 'link', 'music', 'video').               |
| **`mediaUrl`**              | <code>string</code>                                                             | Music or video URL (for type 'music', 'video').                                 |
| **`miniProgramUsername`**   | <code>string</code>                                                             | Mini-program username (for type 'miniprogram').                                 |
| **`miniProgramPath`**       | <code>string</code>                                                             | Mini-program path (for type 'miniprogram').                                     |
| **`miniProgramType`**       | <code>number</code>                                                             | Mini-program type: 0 = Release, 1 = Test, 2 = Preview (for type 'miniprogram'). |
| **`miniProgramWebPageUrl`** | <code>string</code>                                                             | Mini-program web page URL fallback (for type 'miniprogram').                    |


#### WechatPaymentOptions

WeChat payment options.

| Prop            | Type                | Description                            |
| --------------- | ------------------- | -------------------------------------- |
| **`partnerId`** | <code>string</code> | Partner ID (merchant ID).              |
| **`prepayId`**  | <code>string</code> | Prepay ID from unified order API.      |
| **`nonceStr`**  | <code>string</code> | Random string.                         |
| **`timeStamp`** | <code>string</code> | Timestamp.                             |
| **`package`**   | <code>string</code> | Package value, typically 'Sign=WXPay'. |
| **`sign`**      | <code>string</code> | Signature.                             |


#### WechatMiniProgramOptions

WeChat mini-program options.

| Prop           | Type                | Description                                            |
| -------------- | ------------------- | ------------------------------------------------------ |
| **`username`** | <code>string</code> | Mini-program username (original ID).                   |
| **`path`**     | <code>string</code> | Path to open in mini-program.                          |
| **`type`**     | <code>number</code> | Mini-program type: 0 = Release, 1 = Test, 2 = Preview. |


#### WechatInvoiceResponse

WeChat invoice response.

| Prop        | Type                             | Description                 |
| ----------- | -------------------------------- | --------------------------- |
| **`cards`** | <code>WechatInvoiceCard[]</code> | Array of selected card IDs. |


#### WechatInvoiceCard

WeChat invoice card item.

| Prop              | Type                | Description                        |
| ----------------- | ------------------- | ---------------------------------- |
| **`cardId`**      | <code>string</code> | The selected card identifier.      |
| **`encryptCode`** | <code>string</code> | Encrypted code returned by WeChat. |


#### WechatInvoiceOptions

WeChat invoice options.

| Prop            | Type                | Description     |
| --------------- | ------------------- | --------------- |
| **`appId`**     | <code>string</code> | App ID.         |
| **`signType`**  | <code>string</code> | Signature type. |
| **`cardSign`**  | <code>string</code> | Card signature. |
| **`timeStamp`** | <code>string</code> | Timestamp.      |
| **`nonceStr`**  | <code>string</code> | Random string.  |

</docgen-api>

## Usage Examples

### Check Installation

```typescript
import { CapacitorWechat } from '@capgo/capacitor-wechat';

const checkWechat = async () => {
  const { installed } = await CapacitorWechat.isInstalled();
  console.log('WeChat installed:', installed);
};
```

### Authentication

```typescript
const loginWithWechat = async () => {
  try {
    const { code } = await CapacitorWechat.auth({
      scope: 'snsapi_userinfo',
      state: 'my_random_state'
    });

    // Send code to your backend to exchange for access token
    const response = await fetch('https://yourapi.com/auth/wechat', {
      method: 'POST',
      body: JSON.stringify({ code })
    });

    const { access_token } = await response.json();
    console.log('Access token:', access_token);
  } catch (error) {
    console.error('WeChat auth failed:', error);
  }
};
```

### Share Content

```typescript
// Share text
const shareText = async () => {
  await CapacitorWechat.share({
    scene: 0, // 0 = Chat, 1 = Moments, 2 = Favorite
    type: 'text',
    text: 'Hello from Capacitor WeChat!'
  });
};

// Share link
const shareLink = async () => {
  await CapacitorWechat.share({
    scene: 1,
    type: 'link',
    title: 'Check out this awesome app!',
    description: 'Built with Capacitor',
    link: 'https://capacitorjs.com',
    thumbUrl: 'https://capacitorjs.com/icon.png'
  });
};
```

### Payment

```typescript
const payWithWechat = async () => {
  // First, get payment parameters from your server
  const paymentParams = await fetchPaymentParamsFromServer();

  try {
    await CapacitorWechat.sendPaymentRequest({
      partnerId: paymentParams.partnerId,
      prepayId: paymentParams.prepayId,
      nonceStr: paymentParams.nonceStr,
      timeStamp: paymentParams.timeStamp,
      package: paymentParams.package,
      sign: paymentParams.sign
    });

    console.log('Payment successful!');
  } catch (error) {
    console.error('Payment failed:', error);
  }
};
```

### Open Mini Program

```typescript
const openMiniProgram = async () => {
  const { extMsg } = await CapacitorWechat.openMiniProgram({
    username: 'gh_xxxxxxxxxxxxx', // Mini program original ID
    path: 'pages/index/index',
    type: 0 // 0 = Release, 1 = Test, 2 = Preview
  });

  console.log('Mini program returned:', extMsg);
};
```

## Important Notes

1. **WeChat SDK**: The plugin pulls the official SDKs via CocoaPods (`WechatOpenSDK`) and Gradle (`com.tencent.mm.opensdk:wechat-sdk-android-without-mta`). If you use a custom build setup, ensure those dependencies stay intact.

2. **App Registration**: You must register your app on the [WeChat Open Platform](https://open.weixin.qq.com/) to get an App ID.

3. **Universal Links (iOS)**: For iOS 13+, you need to configure Universal Links for WeChat callbacks.

4. **Backend Integration**: Authentication and payment features require backend integration to exchange codes for tokens and prepare payment parameters.

## Troubleshooting

### iOS: App crashes with NSException on startup

**Problem:** App crashes immediately when WeChat plugin loads with an NSException error.

**Solution:** This usually happens when the `universalLink` is not configured or is empty. Ensure you have set the `universalLink` in your `capacitor.config` or when calling `initialize()`:

```typescript
// In capacitor.config.ts
plugins: {
  CapacitorWechat: {
    appId: 'wx1234567890abcdef',
    universalLink: 'https://your-domain.com/universal-link/'  // Required for iOS!
  }
}

// Or in your app initialization
await CapacitorWechat.initialize({
  appId: 'wx1234567890abcdef',
  universalLink: 'https://your-domain.com/universal-link/'
});
```

Learn how to set up Universal Links for WeChat in the [WeChat iOS documentation](https://developers.weixin.qq.com/doc/oplatform/en/Mobile_App/Access_Guide/iOS.html).

### iOS: Build errors with WechatOpenSDK

**Problem:** Build fails with errors about missing WechatOpenSDK module.

**Solution:** Make sure the WeChat SDK is properly installed via CocoaPods. Run:

```bash
cd ios/App
pod install
```

## Credits

This plugin was inspired by [cordova-plugin-wechat](https://github.com/xu-li/cordova-plugin-wechat) and adapted for Capacitor.

## License

MIT

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for details on how to contribute to this plugin.
