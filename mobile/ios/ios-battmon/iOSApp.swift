import SwiftUI
import Firebase

@main
struct iOSApp: App {
	@UIApplicationDelegateAdaptor(AppDelegate.self) var delegate

	var body: some Scene {
		WindowGroup {
			ContentView()
				.onAppear {
					// Initialize notification service
					NotificationServiceBridge.shared.initialize()
				}
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
		// Firebase will handle the token conversion
	}

	func application(
		_ application: UIApplication,
		didFailToRegisterForRemoteNotificationsWithError error: Error
	) {
		print("Failed to register for remote notifications: \(error.localizedDescription)")
	}
}