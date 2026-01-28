// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "CapgoCapacitorWechat",
    platforms: [.iOS(.v15)],
    products: [
        .library(
            name: "CapgoCapacitorWechat",
            targets: ["CapacitorWechatPlugin"])
    ],
    dependencies: [
        .package(url: "https://github.com/ionic-team/capacitor-swift-pm.git", from: "8.0.0"),
        .package(url: "https://github.com/yanyin1986/WechatOpenSDK.git", from: "2.0.7")
    ],
    targets: [
        .target(
            name: "CapacitorWechatPlugin",
            dependencies: [
                .product(name: "Capacitor", package: "capacitor-swift-pm"),
                .product(name: "Cordova", package: "capacitor-swift-pm"),
                .product(name: "WechatOpenSDK", package: "WechatOpenSDK")
            ],
            path: "ios/Sources/CapacitorWechatPlugin"),
        .testTarget(
            name: "CapacitorWechatPluginTests",
            dependencies: ["CapacitorWechatPlugin"],
            path: "ios/Tests/CapacitorWechatPluginTests")
    ]
)
