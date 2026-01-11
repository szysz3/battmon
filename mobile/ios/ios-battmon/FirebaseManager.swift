import SwiftUI
import Firebase
import FirebaseMessaging
import UserNotifications

class FirebaseManager: NSObject, ObservableObject {
    static let shared = FirebaseManager()

    @Published var fcmToken: String?
    private var permissionCallback: ((Bool) -> Void)?
    private var tokenCallback: ((String?) -> Void)?

    override private init() {
        super.init()
    }

    func configure() {
        FirebaseApp.configure()
        Messaging.messaging().delegate = self
        UNUserNotificationCenter.current().delegate = self

        print("Firebase configured successfully")
    }

    func requestNotificationPermission(completion: @escaping (Bool) -> Void) {
        self.permissionCallback = completion

        let authOptions: UNAuthorizationOptions = [.alert, .badge, .sound]
        UNUserNotificationCenter.current().requestAuthorization(
            options: authOptions,
            completionHandler: { granted, error in
                if let error = error {
                    print("Error requesting notification permission: \(error.localizedDescription)")
                    completion(false)
                    return
                }

                print("Notification permission granted: \(granted)")

                if granted {
                    DispatchQueue.main.async {
                        UIApplication.shared.registerForRemoteNotifications()
                    }
                }

                completion(granted)
            }
        )
    }

    func getFCMToken(completion: @escaping (String?) -> Void) {
        self.tokenCallback = completion

        Messaging.messaging().token { token, error in
            if let error = error {
                print("Error fetching FCM registration token: \(error)")
                completion(nil)
            } else if let token = token {
                print("FCM registration token: \(token)")
                self.fcmToken = token
                completion(token)
            }
        }
    }
}

// MARK: - MessagingDelegate
extension FirebaseManager: MessagingDelegate {
    func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
        print("Firebase registration token: \(String(describing: fcmToken))")

        self.fcmToken = fcmToken

        let dataDict: [String: String] = ["token": fcmToken ?? ""]
        NotificationCenter.default.post(
            name: Notification.Name("FCMToken"),
            object: nil,
            userInfo: dataDict
        )

        // Call the stored callback if it exists
        if let token = fcmToken {
            tokenCallback?(token)
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
