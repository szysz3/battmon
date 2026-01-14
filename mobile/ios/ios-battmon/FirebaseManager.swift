import SwiftUI
import Firebase
import FirebaseMessaging
import UserNotifications

class FirebaseManager: NSObject, ObservableObject {
    static let shared = FirebaseManager()

    @Published var fcmToken: String?

    /// Callback to be invoked when a valid FCM token is received (after APNS is set)
    private var onTokenReceived: ((String) -> Void)?

    /// Tracks whether we've already registered with the backend for this token
    private var lastRegisteredToken: String?

    override private init() {
        super.init()
    }

    func configure() {
        FirebaseApp.configure()
        Messaging.messaging().delegate = self
        UNUserNotificationCenter.current().delegate = self

        print("Firebase configured successfully")
    }

    /// Requests notification permission and registers for remote notifications.
    /// The token callback will be invoked via MessagingDelegate once APNS is set and FCM token is ready.
    func requestNotificationPermission(onToken: @escaping (String) -> Void) {
        self.onTokenReceived = onToken

        let authOptions: UNAuthorizationOptions = [.alert, .badge, .sound]
        UNUserNotificationCenter.current().requestAuthorization(
            options: authOptions,
            completionHandler: { granted, error in
                if let error = error {
                    print("Error requesting notification permission: \(error.localizedDescription)")
                    return
                }

                print("Notification permission granted: \(granted)")

                if granted {
                    DispatchQueue.main.async {
                        // This triggers APNS registration
                        // The APNS token will be received in AppDelegate.didRegisterForRemoteNotificationsWithDeviceToken
                        // Then Firebase will generate the FCM token and call MessagingDelegate.didReceiveRegistrationToken
                        UIApplication.shared.registerForRemoteNotifications()
                    }
                }
            }
        )
    }

    /// Sets the callback for when FCM token is received.
    /// If a token is already available, calls the callback immediately.
    func setTokenCallback(_ callback: @escaping (String) -> Void) {
        self.onTokenReceived = callback

        // If we already have a token, invoke callback immediately
        if let token = fcmToken {
            callback(token)
        }
    }
}

// MARK: - MessagingDelegate
extension FirebaseManager: MessagingDelegate {
    func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
        guard let token = fcmToken else {
            print("Firebase: Received nil FCM token")
            return
        }

        print("Firebase: FCM token received: \(token)")
        self.fcmToken = token

        // Post notification for any observers
        NotificationCenter.default.post(
            name: Notification.Name("FCMToken"),
            object: nil,
            userInfo: ["token": token]
        )

        // Invoke the callback if set and token is new
        if let callback = onTokenReceived, lastRegisteredToken != token {
            lastRegisteredToken = token
            callback(token)
        }
    }
}

// MARK: - UNUserNotificationCenterDelegate
extension FirebaseManager: UNUserNotificationCenterDelegate {
    // Receive displayed notifications for iOS 10 devices.
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        let userInfo = notification.request.content.userInfo

        print("Will present notification with userInfo: \(userInfo)")

        // Show notification even when app is in foreground
        completionHandler([[.banner, .badge, .sound]])
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        let userInfo = response.notification.request.content.userInfo

        print("Did receive notification response with userInfo: \(userInfo)")

        completionHandler()
    }
}
