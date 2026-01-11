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
        FirebaseManager.shared.requestNotificationPermission { granted in
            if granted {
                print("NotificationServiceBridge: Permission granted, fetching FCM token")
                NotificationServiceBridge.shared.registerFCMToken()
            } else {
                print("NotificationServiceBridge: Permission denied")
            }
        }
    }

    private func registerFCMToken() {
        FirebaseManager.shared.getFCMToken { token in
            guard let token = token else {
                print("NotificationServiceBridge: Failed to get FCM token")
                return
            }

            print("NotificationServiceBridge: Got FCM token, registering with backend")

            // Create token manager and register
            let tokenManager = NotificationTokenManager(api: BattmonApi())
            tokenManager.registerToken(
                fcmToken: token,
                deviceName: UIDevice.current.name,
                platform: "ios"
            )
        }
    }
}
