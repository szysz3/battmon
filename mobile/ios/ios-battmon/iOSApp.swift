import SwiftUI
import Firebase
import FirebaseMessaging

@main
struct iOSApp: App {
	@UIApplicationDelegateAdaptor(AppDelegate.self) var delegate
	@Environment(\.scenePhase) private var scenePhase

	var body: some Scene {
		WindowGroup {
			ContentView()
				.onAppear {
					NotificationServiceBridge.shared.initialize()
					clearBadge()
				}
				.onChange(of: scenePhase) { phase in
					if phase == .active {
						clearBadge()
					}
				}
		}
	}

	private func clearBadge() {
		DispatchQueue.main.async {
			UIApplication.shared.applicationIconBadgeNumber = 0
		}
	}
}

class AppDelegate: NSObject, UIApplicationDelegate {
	func application(
		_ application: UIApplication,
		didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey : Any]? = nil
	) -> Bool {
		FirebaseManager.shared.configure()
		return true
	}

	func application(
		_ application: UIApplication,
		didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data
	) {
		print("APNs device token: \(deviceToken)")
		Messaging.messaging().apnsToken = deviceToken
	}

	func application(
		_ application: UIApplication,
		didFailToRegisterForRemoteNotificationsWithError error: Error
	) {
		print("Failed to register for remote notifications: \(error.localizedDescription)")
	}
}
