package ee.forgr.plugin.capacitor_wechat;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.tencent.mm.opensdk.constants.ConstantsAPI;
import com.tencent.mm.opensdk.modelbase.BaseReq;
import com.tencent.mm.opensdk.modelbase.BaseResp;
import com.tencent.mm.opensdk.modelbiz.ChooseCardFromWXCardPackage;
import com.tencent.mm.opensdk.modelbiz.WXLaunchMiniProgram;
import com.tencent.mm.opensdk.modelmsg.SendAuth;
import com.tencent.mm.opensdk.modelmsg.SendMessageToWX;
import com.tencent.mm.opensdk.modelmsg.WXImageObject;
import com.tencent.mm.opensdk.modelmsg.WXMediaMessage;
import com.tencent.mm.opensdk.modelmsg.WXMiniProgramObject;
import com.tencent.mm.opensdk.modelmsg.WXMusicObject;
import com.tencent.mm.opensdk.modelmsg.WXTextObject;
import com.tencent.mm.opensdk.modelmsg.WXVideoObject;
import com.tencent.mm.opensdk.modelmsg.WXWebpageObject;
import com.tencent.mm.opensdk.modelpay.PayReq;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONArray;
import org.json.JSONException;

@CapacitorPlugin(name = "CapacitorWechat")
public class CapacitorWechatPlugin extends Plugin implements WechatResponseListener {

    private static final String TAG = "CapacitorWechat";

    private static final int REQUEST_TYPE_SHARE = ConstantsAPI.COMMAND_SENDMESSAGE_TO_WX;
    private static final int REQUEST_TYPE_AUTH = ConstantsAPI.COMMAND_SENDAUTH;
    private static final int REQUEST_TYPE_PAY = ConstantsAPI.COMMAND_PAY_BY_WX;
    private static final int REQUEST_TYPE_MINI_PROGRAM = ConstantsAPI.COMMAND_LAUNCH_WX_MINIPROGRAM;
    private static final int REQUEST_TYPE_INVOICE = ConstantsAPI.COMMAND_CHOOSE_CARD_FROM_EX_CARD_PACKAGE;

    private static final int MINI_PROGRAM_TYPE_RELEASE = 0;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Map<Integer, PluginCall> pendingCalls = new ConcurrentHashMap<>();

    private String currentAppId;
    private String universalLink;
    private IWXAPI api;
    private final String pluginVersion = "8.0.38";

    @Override
    public void load() {
        super.load();
        WechatManager.getInstance().registerListener(this);

        Context context = getContext();
        if (context == null) {
            return;
        }

        String configuredAppId = getConfig().getString("appId", null);
        String configuredUniversalLink = getConfig().getString("universalLink", null);
        if (TextUtils.isEmpty(configuredAppId)) {
            configuredAppId = WechatPreferences.getAppId(context);
            configuredUniversalLink = WechatPreferences.getUniversalLink(context);
        }

        if (!TextUtils.isEmpty(configuredAppId)) {
            configureSdk(configuredAppId, configuredUniversalLink, false);
        }
    }

    @Override
    protected void handleOnDestroy() {
        super.handleOnDestroy();
        executor.shutdown();
        pendingCalls.clear();
        WechatManager.getInstance().unregisterListener(this);
    }

    @PluginMethod
    public void initialize(PluginCall call) {
        String appId = call.getString("appId");
        if (TextUtils.isEmpty(appId)) {
            call.reject(WechatConstants.ERROR_APP_ID_MISSING);
            return;
        }
        String universal = call.getString("universalLink");
        if (!configureSdk(appId, universal, true)) {
            call.reject(WechatConstants.ERROR_SDK_NOT_READY);
            return;
        }
        call.resolve();
    }

    @PluginMethod
    public void isInstalled(PluginCall call) {
        if (!ensureReady(call, false)) {
            return;
        }
        boolean installed = api != null && api.isWXAppInstalled();
        JSObject result = new JSObject();
        result.put("installed", installed);
        call.resolve(result);
    }

    @PluginMethod
    public void auth(PluginCall call) {
        if (!ensureReady(call, true) || !ensureWechatInstalled(call)) {
            return;
        }
        String scope = call.getString("scope");
        String state = call.getString("state");
        if (TextUtils.isEmpty(scope)) {
            call.reject("Missing scope parameter.");
            return;
        }
        SendAuth.Req req = new SendAuth.Req();
        req.scope = scope;
        req.state = TextUtils.isEmpty(state) ? UUID.randomUUID().toString() : state;

        if (!registerPendingCall(REQUEST_TYPE_AUTH, call)) {
            return;
        }
        sendRequestOrReject(req, REQUEST_TYPE_AUTH, call);
    }

    @PluginMethod
    public void share(PluginCall call) {
        if (!ensureReady(call, true) || !ensureWechatInstalled(call)) {
            return;
        }

        Integer scene = call.getInt("scene");
        String type = call.getString("type");
        if (scene == null || TextUtils.isEmpty(type)) {
            call.reject(WechatConstants.ERROR_INVALID_ARGUMENTS);
            return;
        }

        if ("text".equals(type)) {
            sendShareRequest(call, buildTextShare(scene, call.getString("text")));
            return;
        }

        executor.execute(() -> {
            try {
                SendMessageToWX.Req req = buildRichShare(call, type, scene);
                mainHandler.post(() -> sendShareRequest(call, req));
            } catch (IllegalArgumentException ex) {
                call.reject(ex.getMessage());
            } catch (IOException ex) {
                call.reject(WechatConstants.ERROR_BITMAP_LOAD, ex);
            }
        });
    }

    @PluginMethod
    public void sendPaymentRequest(PluginCall call) {
        if (!ensureReady(call, true) || !ensureWechatInstalled(call)) {
            return;
        }
        String partnerId = call.getString("partnerId");
        String prepayId = call.getString("prepayId");
        String nonceStr = call.getString("nonceStr");
        String timeStamp = call.getString("timeStamp");
        String packageValue = call.getString("package");
        String sign = call.getString("sign");

        if (
            TextUtils.isEmpty(partnerId) ||
            TextUtils.isEmpty(prepayId) ||
            TextUtils.isEmpty(nonceStr) ||
            TextUtils.isEmpty(timeStamp) ||
            TextUtils.isEmpty(packageValue) ||
            TextUtils.isEmpty(sign)
        ) {
            call.reject(WechatConstants.ERROR_INVALID_ARGUMENTS);
            return;
        }

        PayReq req = new PayReq();
        req.appId = currentAppId;
        req.partnerId = partnerId;
        req.prepayId = prepayId;
        req.nonceStr = nonceStr;
        req.timeStamp = timeStamp;
        req.packageValue = packageValue;
        req.sign = sign;

        if (!registerPendingCall(REQUEST_TYPE_PAY, call)) {
            return;
        }
        sendRequestOrReject(req, REQUEST_TYPE_PAY, call);
    }

    @PluginMethod
    public void openMiniProgram(PluginCall call) {
        if (!ensureReady(call, true) || !ensureWechatInstalled(call)) {
            return;
        }
        String username = call.getString("username");
        if (TextUtils.isEmpty(username)) {
            call.reject("Missing username parameter.");
            return;
        }
        WXLaunchMiniProgram.Req req = new WXLaunchMiniProgram.Req();
        req.userName = username;
        req.path = call.getString("path");
        req.miniprogramType = call.getInt("type", 0);

        if (!registerPendingCall(REQUEST_TYPE_MINI_PROGRAM, call)) {
            return;
        }
        sendRequestOrReject(req, REQUEST_TYPE_MINI_PROGRAM, call);
    }

    @PluginMethod
    public void chooseInvoice(PluginCall call) {
        if (!ensureReady(call, true) || !ensureWechatInstalled(call)) {
            return;
        }
        String appId = call.getString("appId");
        String signType = call.getString("signType");
        String cardSign = call.getString("cardSign");
        String timeStamp = call.getString("timeStamp");
        String nonceStr = call.getString("nonceStr");

        if (
            TextUtils.isEmpty(appId) ||
            TextUtils.isEmpty(signType) ||
            TextUtils.isEmpty(cardSign) ||
            TextUtils.isEmpty(timeStamp) ||
            TextUtils.isEmpty(nonceStr)
        ) {
            call.reject(WechatConstants.ERROR_INVALID_ARGUMENTS);
            return;
        }

        ChooseCardFromWXCardPackage.Req req = new ChooseCardFromWXCardPackage.Req();
        req.appId = appId;
        req.cardSign = cardSign;
        req.nonceStr = nonceStr;
        req.signType = signType;
        req.timeStamp = timeStamp;
        req.canMultiSelect = "1";

        if (!registerPendingCall(REQUEST_TYPE_INVOICE, call)) {
            return;
        }
        sendRequestOrReject(req, REQUEST_TYPE_INVOICE, call);
    }

    @PluginMethod
    public void getPluginVersion(final PluginCall call) {
        try {
            final JSObject ret = new JSObject();
            ret.put("version", this.pluginVersion);
            call.resolve(ret);
        } catch (final Exception e) {
            call.reject("Could not get plugin version", e);
        }
    }

    @Override
    public void onWechatResponse(BaseResp resp) {
        PluginCall pending = pendingCalls.remove(resp.getType());
        if (pending == null) {
            Log.w(TAG, "No pending call for response type " + resp.getType());
            return;
        }
        pending.setKeepAlive(false);
        switch (resp.errCode) {
            case BaseResp.ErrCode.ERR_OK:
                handleSuccessResponse(resp, pending);
                break;
            case BaseResp.ErrCode.ERR_USER_CANCEL:
                pending.reject("User cancelled", String.valueOf(resp.errCode));
                break;
            case BaseResp.ErrCode.ERR_AUTH_DENIED:
                pending.reject("Authorization denied", String.valueOf(resp.errCode));
                break;
            case BaseResp.ErrCode.ERR_SENT_FAILED:
                pending.reject("Send request failed", String.valueOf(resp.errCode));
                break;
            case BaseResp.ErrCode.ERR_UNSUPPORT:
                pending.reject("Operation not supported by WeChat", String.valueOf(resp.errCode));
                break;
            case BaseResp.ErrCode.ERR_COMM:
            default:
                pending.reject("WeChat error (" + resp.errCode + ")", String.valueOf(resp.errCode));
                break;
        }
    }

    @Override
    public void onWechatRequest(BaseReq baseReq) {
        Log.d(TAG, "Received WeChat request of type " + baseReq.getType());
    }

    private boolean ensureReady(PluginCall call, boolean requireAppId) {
        if (requireAppId && TextUtils.isEmpty(currentAppId)) {
            call.reject(WechatConstants.ERROR_NOT_CONFIGURED);
            return false;
        }
        if (api == null) {
            Context context = getContext();
            if (context == null) {
                call.reject(WechatConstants.ERROR_SDK_NOT_READY);
                return false;
            }
            api = WechatManager.getInstance().getOrCreateApi(context);
            if (api == null && requireAppId) {
                if (!configureSdk(currentAppId, universalLink, false)) {
                    call.reject(WechatConstants.ERROR_SDK_NOT_READY);
                    return false;
                }
            }
        }
        return true;
    }

    private boolean ensureWechatInstalled(PluginCall call) {
        if (api == null || !api.isWXAppInstalled()) {
            call.reject(WechatConstants.ERROR_WECHAT_NOT_INSTALLED);
            return false;
        }
        return true;
    }

    private boolean configureSdk(String appId, String universal, boolean persist) {
        Context context = getContext();
        if (context == null) {
            return false;
        }
        currentAppId = appId;
        universalLink = universal;
        WechatManager.getInstance().configure(context, appId);
        api = WechatManager.getInstance().getApi();
        if (api == null) {
            return false;
        }
        if (persist) {
            WechatPreferences.persist(context, appId, universal);
        }
        return true;
    }

    private SendMessageToWX.Req buildTextShare(int scene, String text) {
        String safeText = TextUtils.isEmpty(text) ? "" : text;
        WXTextObject textObject = new WXTextObject();
        textObject.text = safeText;
        WXMediaMessage message = new WXMediaMessage();
        message.mediaObject = textObject;
        message.description = safeText;

        SendMessageToWX.Req req = new SendMessageToWX.Req();
        req.transaction = buildTransaction("text");
        req.message = message;
        req.scene = scene;
        return req;
    }

    private SendMessageToWX.Req buildRichShare(PluginCall call, String type, int scene) throws IOException {
        Context context = getContext();
        if (context == null) {
            throw new IllegalStateException("Context unavailable.");
        }
        WXMediaMessage message = new WXMediaMessage();
        switch (type) {
            case "image": {
                String imageUrl = call.getString("imageUrl");
                if (TextUtils.isEmpty(imageUrl)) {
                    throw new IllegalArgumentException("imageUrl is required for image shares.");
                }
                Bitmap bitmap = WechatImageHelper.loadBitmap(context, imageUrl);
                if (bitmap == null) {
                    throw new IOException("Unable to decode image.");
                }
                WXImageObject imageObject = new WXImageObject(bitmap);
                message.mediaObject = imageObject;
                message.thumbData = WechatImageHelper.buildThumbnail(WechatImageHelper.scaleDown(bitmap, 1280));
                break;
            }
            case "link": {
                String link = call.getString("link");
                if (TextUtils.isEmpty(link)) {
                    throw new IllegalArgumentException("link is required for link shares.");
                }
                WXWebpageObject webpage = new WXWebpageObject();
                webpage.webpageUrl = link;
                message.mediaObject = webpage;
                message.title = call.getString("title");
                message.description = call.getString("description");
                attachThumbIfPresent(context, message, call.getString("thumbUrl"));
                break;
            }
            case "music": {
                String mediaUrl = call.getString("mediaUrl");
                if (TextUtils.isEmpty(mediaUrl)) {
                    throw new IllegalArgumentException("mediaUrl is required for music shares.");
                }
                WXMusicObject music = new WXMusicObject();
                music.musicUrl = mediaUrl;
                message.mediaObject = music;
                message.title = call.getString("title");
                message.description = call.getString("description");
                attachThumbIfPresent(context, message, call.getString("thumbUrl"));
                break;
            }
            case "video": {
                String videoUrl = call.getString("mediaUrl");
                if (TextUtils.isEmpty(videoUrl)) {
                    throw new IllegalArgumentException("mediaUrl is required for video shares.");
                }
                WXVideoObject video = new WXVideoObject();
                video.videoUrl = videoUrl;
                message.mediaObject = video;
                message.title = call.getString("title");
                message.description = call.getString("description");
                attachThumbIfPresent(context, message, call.getString("thumbUrl"));
                break;
            }
            case "miniprogram": {
                String username = call.getString("miniProgramUsername");
                if (TextUtils.isEmpty(username)) {
                    throw new IllegalArgumentException("miniProgramUsername is required for mini program shares.");
                }
                WXMiniProgramObject mini = new WXMiniProgramObject();
                mini.userName = username;
                mini.path = call.getString("miniProgramPath");
                mini.miniprogramType = call.getInt("miniProgramType", MINI_PROGRAM_TYPE_RELEASE);
                mini.webpageUrl = call.getString("miniProgramWebPageUrl");
                message.mediaObject = mini;
                message.title = call.getString("title");
                message.description = call.getString("description");
                String hdImage = call.getString("imageUrl");
                if (!TextUtils.isEmpty(hdImage)) {
                    Bitmap bitmap = WechatImageHelper.loadBitmap(context, hdImage);
                    if (bitmap != null) {
                        message.thumbData = WechatImageHelper.buildThumbnail(WechatImageHelper.scaleDown(bitmap, 512));
                    }
                }
                attachThumbIfPresent(context, message, call.getString("thumbUrl"));
                scene = SendMessageToWX.Req.WXSceneSession;
                break;
            }
            default:
                throw new IllegalArgumentException("Unsupported share type: " + type);
        }

        SendMessageToWX.Req req = new SendMessageToWX.Req();
        req.transaction = buildTransaction(type);
        req.message = message;
        req.scene = scene;
        return req;
    }

    private void attachThumbIfPresent(Context context, WXMediaMessage message, String thumbSource) throws IOException {
        if (TextUtils.isEmpty(thumbSource)) {
            return;
        }
        Bitmap bitmap = WechatImageHelper.loadBitmap(context, thumbSource);
        if (bitmap != null) {
            message.thumbData = WechatImageHelper.buildThumbnail(WechatImageHelper.scaleDown(bitmap, 512));
        }
    }

    private void sendShareRequest(PluginCall call, SendMessageToWX.Req req) {
        if (req == null) {
            call.reject(WechatConstants.ERROR_INVALID_ARGUMENTS);
            return;
        }
        if (!registerPendingCall(REQUEST_TYPE_SHARE, call)) {
            return;
        }
        sendRequestOrReject(req, REQUEST_TYPE_SHARE, call);
    }

    private void sendRequestOrReject(Object request, int requestType, PluginCall call) {
        if (api == null) {
            call.reject(WechatConstants.ERROR_SDK_NOT_READY);
            pendingCalls.remove(requestType);
            return;
        }
        boolean sent = false;
        if (request instanceof SendMessageToWX.Req) {
            sent = api.sendReq((SendMessageToWX.Req) request);
        } else if (request instanceof SendAuth.Req) {
            sent = api.sendReq((SendAuth.Req) request);
        } else if (request instanceof PayReq) {
            sent = api.sendReq((PayReq) request);
        } else if (request instanceof WXLaunchMiniProgram.Req) {
            sent = api.sendReq((WXLaunchMiniProgram.Req) request);
        } else if (request instanceof ChooseCardFromWXCardPackage.Req) {
            sent = api.sendReq((ChooseCardFromWXCardPackage.Req) request);
        }

        if (!sent) {
            pendingCalls.remove(requestType);
            call.setKeepAlive(false);
            call.reject(WechatConstants.ERROR_REQUEST_FAILED);
        } else {
            call.setKeepAlive(true);
        }
    }

    private boolean registerPendingCall(int type, PluginCall call) {
        PluginCall existingCall = pendingCalls.get(type);
        if (existingCall != null) {
            // Clean up the existing stale call by rejecting it
            existingCall.setKeepAlive(false);
            existingCall.reject("Request superseded by new request");
            Log.w(TAG, "Replaced stale pending call of type " + type);
        }
        pendingCalls.put(type, call);
        return true;
    }

    private void handleSuccessResponse(BaseResp resp, PluginCall call) {
        if (resp instanceof SendAuth.Resp) {
            SendAuth.Resp authResp = (SendAuth.Resp) resp;
            JSObject result = new JSObject();
            result.put("code", authResp.code);
            result.put("state", authResp.state);
            call.resolve(result);
            return;
        }
        if (resp instanceof WXLaunchMiniProgram.Resp) {
            WXLaunchMiniProgram.Resp miniResp = (WXLaunchMiniProgram.Resp) resp;
            JSObject result = new JSObject();
            result.put("extMsg", miniResp.extMsg);
            call.resolve(result);
            return;
        }
        if (resp instanceof ChooseCardFromWXCardPackage.Resp) {
            ChooseCardFromWXCardPackage.Resp invoiceResp = (ChooseCardFromWXCardPackage.Resp) resp;
            JSArray cards = parseCardList(invoiceResp.cardItemList);
            JSObject result = new JSObject();
            result.put("cards", cards);
            call.resolve(result);
            return;
        }
        call.resolve();
    }

    private JSArray parseCardList(String cardItemList) {
        JSArray array = new JSArray();
        if (TextUtils.isEmpty(cardItemList)) {
            return array;
        }
        try {
            JSONArray jsonArray = new JSONArray(cardItemList);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSObject card = new JSObject();
                card.put("cardId", jsonArray.getJSONObject(i).optString("card_id"));
                card.put("encryptCode", jsonArray.getJSONObject(i).optString("encrypt_code"));
                array.put(card);
            }
        } catch (JSONException e) {
            Log.e(TAG, "Failed to parse card list", e);
        }
        return array;
    }

    private String buildTransaction(String type) {
        return (type == null ? "" : type) + System.currentTimeMillis();
    }
}
