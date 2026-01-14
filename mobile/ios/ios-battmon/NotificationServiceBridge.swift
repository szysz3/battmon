import Foundation
import UIKit
import shared

@objc public class NotificationServiceBridge: NSObject {
    @objc public static let shared = NotificationServiceBridge()

    private override init() {
        super.init()
    }

    /// Initializes push notifications by requesting permission and registering the FCM token.
    /// The FCM token registration with the backend happens only after:
    /// 1. User grants notification permission
    /// 2. APNS registration completes (device token received)
    /// 3. Firebase generates the FCM token
    @objc public func initialize() {
        print("NotificationServiceBridge: Requesting notification permission")

        let tokenHandler: (String) -> Void = { [weak self] token in
            self?.registerWithBackend(token: token)
        }

        // Ensure we handle tokens that might already be available.
        FirebaseManager.shared.setTokenCallback(tokenHandler)

        // Pass a callback that will be invoked when FCM token is ready
        // This happens AFTER APNS registration completes and Firebase generates the token
        FirebaseManager.shared.requestNotificationPermission(onToken: tokenHandler)
    }

    private func registerWithBackend(token: String) {
        print("NotificationServiceBridge: FCM token ready, registering with backend")

        // Create token manager and register
        let tokenManager = NotificationTokenManager(api: BattmonApi())
        tokenManager.registerToken(
            fcmToken: token,
            deviceName: UIDevice.current.name,
            platform: "ios"
        )
    }
}
