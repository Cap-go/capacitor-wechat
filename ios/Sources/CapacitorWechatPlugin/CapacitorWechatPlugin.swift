import Foundation
import Capacitor

@objc(CapacitorWechatPlugin)
public class CapacitorWechatPlugin: CAPPlugin, CAPBridgedPlugin {
    private let pluginVersion: String = "8.0.10"
    public let identifier = "CapacitorWechatPlugin"
    public let jsName = "CapacitorWechat"
    public let pluginMethods: [CAPPluginMethod] = [
        CAPPluginMethod(name: "initialize", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "isInstalled", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "auth", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "share", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "sendPaymentRequest", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "openMiniProgram", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "chooseInvoice", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getPluginVersion", returnType: CAPPluginReturnPromise)
    ]

    private let implementation = CapacitorWechat.shared

    override public func load() {
        super.load()
        NotificationCenter.default.addObserver(self, selector: #selector(handleOpenURL(_:)), name: .capacitorOpenURL, object: nil)
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(handleContinueUserActivity(_:)),
            name: Notification.Name("capacitorContinueUserActivity"),
            object: nil
        )

        implementation.bootstrap(appId: getConfig().getString("appId"), universalLink: getConfig().getString("universalLink"))
    }

    deinit {
        NotificationCenter.default.removeObserver(self)
    }

    @objc func initialize(_ call: CAPPluginCall) {
        guard let appId = call.getString("appId") else {
            call.reject("Missing appId parameter.")
            return
        }
        do {
            try implementation.configure(appId: appId, universalLink: call.getString("universalLink"))
            call.resolve()
        } catch {
            call.reject(error.localizedDescription, nil, error)
        }
    }

    @objc func isInstalled(_ call: CAPPluginCall) {
        let installed = implementation.isWechatInstalled()
        call.resolve(["installed": installed])
    }

    @objc func auth(_ call: CAPPluginCall) {
        guard let scope = call.getString("scope") else {
            call.reject("Missing scope parameter.")
            return
        }
        guard let presenter = bridge?.viewController else {
            call.reject("No active view controller to present WeChat UI.")
            return
        }

        do {
            try implementation.auth(scope: scope, state: call.getString("state"), presenter: presenter) { result in
                switch result {
                case .success(let response):
                    call.resolve([
                        "code": response.code,
                        "state": response.state ?? ""
                    ])
                case .failure(let error):
                    call.reject(error.localizedDescription, nil, error)
                }
            }
        } catch {
            call.reject(error.localizedDescription, nil, error)
        }
    }

    @objc func share(_ call: CAPPluginCall) {
        guard let scene = call.getInt("scene") else {
            call.reject("Missing scene parameter.")
            return
        }

        let options = WechatShareOptions(
            scene: scene,
            type: call.getString("type"),
            text: call.getString("text"),
            title: call.getString("title"),
            description: call.getString("description"),
            link: call.getString("link"),
            imageUrl: call.getString("imageUrl"),
            thumbUrl: call.getString("thumbUrl"),
            mediaUrl: call.getString("mediaUrl"),
            miniProgramUsername: call.getString("miniProgramUsername"),
            miniProgramPath: call.getString("miniProgramPath"),
            miniProgramType: call.getInt("miniProgramType"),
            miniProgramWebPageUrl: call.getString("miniProgramWebPageUrl")
        )

        do {
            try implementation.share(options: options) { result in
                switch result {
                case .success:
                    call.resolve()
                case .failure(let error):
                    call.reject(error.localizedDescription, nil, error)
                }
            }
        } catch {
            call.reject(error.localizedDescription, nil, error)
        }
    }

    @objc func sendPaymentRequest(_ call: CAPPluginCall) {
        guard let partnerId = call.getString("partnerId"),
              let prepayId = call.getString("prepayId"),
              let nonceStr = call.getString("nonceStr"),
              let timeStamp = call.getString("timeStamp"),
              let packageValue = call.getString("package"),
              let sign = call.getString("sign") else {
            call.reject("Missing required payment parameters.")
            return
        }

        let options = WechatPaymentOptions(
            partnerId: partnerId,
            prepayId: prepayId,
            nonceStr: nonceStr,
            timeStamp: timeStamp,
            package: packageValue,
            sign: sign
        )

        do {
            try implementation.sendPaymentRequest(options: options) { result in
                switch result {
                case .success:
                    call.resolve()
                case .failure(let error):
                    call.reject(error.localizedDescription, nil, error)
                }
            }
        } catch {
            call.reject(error.localizedDescription, nil, error)
        }
    }

    @objc func openMiniProgram(_ call: CAPPluginCall) {
        guard let username = call.getString("username") else {
            call.reject("Missing username parameter.")
            return
        }

        let options = WechatMiniProgramOptions(
            username: username,
            path: call.getString("path"),
            type: call.getInt("type")
        )

        do {
            try implementation.openMiniProgram(options: options) { result in
                switch result {
                case .success(let extMsg):
                    var payload: [String: String] = [:]
                    if let extMsg, !extMsg.isEmpty {
                        payload["extMsg"] = extMsg
                    }
                    call.resolve(payload)
                case .failure(let error):
                    call.reject(error.localizedDescription, nil, error)
                }
            }
        } catch {
            call.reject(error.localizedDescription, nil, error)
        }
    }

    @objc func chooseInvoice(_ call: CAPPluginCall) {
        guard let appId = call.getString("appId"),
              let signType = call.getString("signType"),
              let cardSign = call.getString("cardSign"),
              let timeStamp = call.getString("timeStamp"),
              let nonceStr = call.getString("nonceStr") else {
            call.reject("Missing required invoice parameters.")
            return
        }

        let options = WechatInvoiceOptions(
            appId: appId,
            signType: signType,
            cardSign: cardSign,
            timeStamp: timeStamp,
            nonceStr: nonceStr
        )

        do {
            try implementation.chooseInvoice(options: options) { result in
                switch result {
                case .success(let cards):
                    call.resolve(["cards": cards])
                case .failure(let error):
                    call.reject(error.localizedDescription, nil, error)
                }
            }
        } catch {
            call.reject(error.localizedDescription, nil, error)
        }
    }

    @objc func getPluginVersion(_ call: CAPPluginCall) {
        call.resolve(["version": self.pluginVersion])
    }

    @objc private func handleOpenURL(_ notification: Notification) {
        guard let object = notification.object as? [String: Any],
              let url = object["url"] as? URL else {
            return
        }
        _ = implementation.handleOpenURL(url)
    }

    @objc private func handleContinueUserActivity(_ notification: Notification) {
        guard let object = notification.object as? [String: Any],
              let activity = object["userActivity"] as? NSUserActivity else {
            return
        }
        _ = implementation.handleUniversalLink(activity)
    }
}
