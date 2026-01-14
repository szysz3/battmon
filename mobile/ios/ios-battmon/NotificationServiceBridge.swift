import Foundation
import UIKit
import shared

@objc public class NotificationServiceBridge: NSObject {
    @objc public static let shared = NotificationServiceBridge()

    private override init() {
        super.init()
    }

    @objc public func initialize() {
        print("NotificationServiceBridge: Requesting notification permission")

        let tokenHandler: (String) -> Void = { [weak self] token in
            self?.registerWithBackend(token: token)
        }

        FirebaseManager.shared.setTokenCallback(tokenHandler)

        FirebaseManager.shared.requestNotificationPermission(onToken: tokenHandler)
    }

    private func registerWithBackend(token: String) {
        print("NotificationServiceBridge: FCM token ready, registering with backend")

        let tokenManager = NotificationTokenManager(api: BattmonApi())
        tokenManager.registerToken(
            fcmToken: token,
            deviceName: UIDevice.current.name,
            platform: "ios"
        )
    }
}
