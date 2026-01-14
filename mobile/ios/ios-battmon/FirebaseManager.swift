import SwiftUI
import Firebase
import FirebaseMessaging
import UserNotifications

class FirebaseManager: NSObject, ObservableObject {
    static let shared = FirebaseManager()

    @Published var fcmToken: String?

    private var onTokenReceived: ((String) -> Void)?

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
                        UIApplication.shared.registerForRemoteNotifications()
                    }
                }
            }
        )
    }

    func setTokenCallback(_ callback: @escaping (String) -> Void) {
        self.onTokenReceived = callback

        if let token = fcmToken {
            callback(token)
        }
    }
}

extension FirebaseManager: MessagingDelegate {
    func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
        guard let token = fcmToken else {
            print("Firebase: Received nil FCM token")
            return
        }

        print("Firebase: FCM token received: \(token)")
        self.fcmToken = token

        NotificationCenter.default.post(
            name: Notification.Name("FCMToken"),
            object: nil,
            userInfo: ["token": token]
        )

        if let callback = onTokenReceived, lastRegisteredToken != token {
            lastRegisteredToken = token
            callback(token)
        }
    }
}

extension FirebaseManager: UNUserNotificationCenterDelegate {
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        let userInfo = notification.request.content.userInfo

        print("Will present notification with userInfo: \(userInfo)")

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
