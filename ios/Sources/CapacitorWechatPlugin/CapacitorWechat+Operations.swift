import Foundation
import UIKit
import WechatOpenSDK

extension CapacitorWechat {
    func bootstrap(appId: String?, universalLink: String?) {
        let storedAppId = appId ?? UserDefaults.standard.string(forKey: storageAppIdKey)
        let storedLink = universalLink ?? UserDefaults.standard.string(forKey: storageUniversalLinkKey)
        if let storedAppId {
            do {
                try configure(appId: storedAppId, universalLink: storedLink)
            } catch {
                // Log the error for debugging but don't crash during bootstrap
                NSLog("CapacitorWechat: Failed to configure during bootstrap: %@", error.localizedDescription)
                NSLog("CapacitorWechat: Make sure to configure universalLink in capacitor.config or call initialize() with valid parameters")
            }
        }
    }

    func configure(appId: String, universalLink: String?) throws {
        // Validate appId is not empty
        guard !appId.isEmpty else {
            throw WechatError.invalidArguments("appId cannot be empty")
        }
        
        // Validate universalLink is provided and not empty
        // WeChat SDK requires a valid universal link for iOS and will crash with empty string
        guard let universalLink = universalLink, !universalLink.isEmpty else {
            throw WechatError.invalidArguments("universalLink is required for iOS. Please configure it in capacitor.config or pass it to initialize()")
        }
        
        self.appId = appId
        self.universalLink = universalLink

        UserDefaults.standard.set(appId, forKey: storageAppIdKey)
        UserDefaults.standard.set(universalLink, forKey: storageUniversalLinkKey)

        DispatchQueue.main.async {
            WXApi.registerApp(appId, universalLink: universalLink)
        }
    }

    func isWechatInstalled() -> Bool {
        WXApi.isWXAppInstalled()
    }

    func auth(scope: String, state: String?, presenter: UIViewController, completion: @escaping (Result<WechatAuthResponse, Error>) -> Void) throws {
        try ensureConfigured()
        guard WXApi.isWXAppInstalled() else {
            throw WechatError.wechatNotInstalled
        }
        guard authCompletion == nil else {
            throw WechatError.operationInProgress("auth")
        }
        authCompletion = completion

        DispatchQueue.main.async {
            let req = SendAuthReq()
            req.scope = scope
            req.state = state ?? UUID().uuidString
            WXApi.sendAuthReq(req, viewController: presenter, delegate: self) { success in
                if !success {
                    self.consumeAuth(with: .failure(WechatError.requestFailed))
                }
            }
        }
    }

    func share(options: WechatShareOptions, completion: @escaping (Result<Void, Error>) -> Void) throws {
        try ensureConfigured()
        guard WXApi.isWXAppInstalled() else {
            throw WechatError.wechatNotInstalled
        }
        guard shareCompletion == nil else {
            throw WechatError.operationInProgress("share")
        }
        shareCompletion = completion

        workerQueue.async {
            do {
                let request = try self.buildShareRequest(options: options)
                DispatchQueue.main.async {
                    WXApi.send(request) { success in
                        if !success {
                            self.consumeShare(with: .failure(WechatError.requestFailed))
                        }
                    }
                }
            } catch {
                self.consumeShare(with: .failure(error))
            }
        }
    }

    func sendPaymentRequest(options: WechatPaymentOptions, completion: @escaping (Result<Void, Error>) -> Void) throws {
        try ensureConfigured()
        guard WXApi.isWXAppInstalled() else {
            throw WechatError.wechatNotInstalled
        }
        guard payCompletion == nil else {
            throw WechatError.operationInProgress("payment")
        }
        guard let timeStamp = UInt32(options.timeStamp) else {
            throw WechatError.invalidArguments("Invalid timestamp value.")
        }

        let req = PayReq()
        req.partnerId = options.partnerId
        req.prepayId = options.prepayId
        req.nonceStr = options.nonceStr
        req.timeStamp = timeStamp
        req.package = options.package
        req.sign = options.sign

        payCompletion = completion
        DispatchQueue.main.async {
            WXApi.send(req) { success in
                if !success {
                    self.consumePay(with: .failure(WechatError.requestFailed))
                }
            }
        }
    }

    func openMiniProgram(options: WechatMiniProgramOptions, completion: @escaping (Result<String?, Error>) -> Void) throws {
        try ensureConfigured()
        guard WXApi.isWXAppInstalled() else {
            throw WechatError.wechatNotInstalled
        }
        guard miniProgramCompletion == nil else {
            throw WechatError.operationInProgress("miniProgram")
        }
        guard !options.username.isEmpty else {
            throw WechatError.invalidArguments("username is required.")
        }

        let req = WXLaunchMiniProgramReq.object()
        req.userName = options.username
        req.path = options.path
        let miniTypeValue = UInt(options.type ?? 0)
        req.miniProgramType = WXMiniProgramType(rawValue: miniTypeValue) ?? .release

        miniProgramCompletion = completion
        DispatchQueue.main.async {
            WXApi.send(req) { success in
                if !success {
                    self.consumeMiniProgram(with: .failure(WechatError.requestFailed))
                }
            }
        }
    }

    func chooseInvoice(options: WechatInvoiceOptions, completion: @escaping (Result<[[String: String]], Error>) -> Void) throws {
        try ensureConfigured()
        guard WXApi.isWXAppInstalled() else {
            throw WechatError.wechatNotInstalled
        }
        guard invoiceCompletion == nil else {
            throw WechatError.operationInProgress("invoice")
        }

        guard
            !options.appId.isEmpty,
            !options.cardSign.isEmpty,
            !options.signType.isEmpty,
            !options.timeStamp.isEmpty,
            !options.nonceStr.isEmpty
        else {
            throw WechatError.invalidArguments("Missing invoice parameters.")
        }

        let req = WXChooseInvoiceReq()
        req.appID = options.appId
        req.cardSign = options.cardSign
        req.nonceStr = options.nonceStr
        req.signType = options.signType
        req.timeStamp = UInt32(options.timeStamp) ?? 0

        invoiceCompletion = completion
        DispatchQueue.main.async {
            WXApi.send(req) { success in
                if !success {
                    self.consumeInvoice(with: .failure(WechatError.requestFailed))
                }
            }
        }
    }

    @discardableResult
    func handleOpenURL(_ url: URL) -> Bool {
        WXApi.handleOpen(url, delegate: self)
    }

    @discardableResult
    func handleUniversalLink(_ activity: NSUserActivity) -> Bool {
        WXApi.handleOpenUniversalLink(activity, delegate: self)
    }
}
