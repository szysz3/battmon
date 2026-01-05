import SwiftUI
import shared
import UIKit

struct ContentView: View {
	var body: some View {
		ComposeView()
			.ignoresSafeArea(.all)
	}
}

struct ContentView_Previews: PreviewProvider {
	static var previews: some View {
		ContentView()
	}
}

struct ComposeView: UIViewControllerRepresentable {
	func makeUIViewController(context: Context) -> UIViewController {
		MainViewControllerKt.MainViewController()
	}

	func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
	}
}
