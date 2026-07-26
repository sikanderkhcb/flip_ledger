package com.circuitflip.flipledger.platform

import androidx.compose.ui.platform.UriHandler
import platform.Foundation.NSURL
import platform.SafariServices.SFSafariViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController

/** Presents hosted checkout inside the iOS app and owns its dismissal. */
object CheckoutBrowser {
    private var safariViewController: SFSafariViewController? = null

    fun open(uri: String) {
        val url = NSURL.URLWithString(uri) ?: return
        val presenter = topViewController() ?: return
        safariViewController?.dismissViewControllerAnimated(false, completion = null)
        safariViewController = SFSafariViewController(url)
        presenter.presentViewController(
            safariViewController!!,
            animated = true,
            completion = null,
        )
    }

    fun dismiss() {
        safariViewController?.dismissViewControllerAnimated(true, completion = null)
        safariViewController = null
    }

    private fun topViewController(): UIViewController? {
        var controller = UIApplication.sharedApplication.keyWindow?.rootViewController
        while (controller?.presentedViewController != null) {
            controller = controller.presentedViewController
        }
        return controller
    }
}

fun createIosUriHandler(): UriHandler = object : UriHandler {
    override fun openUri(uri: String) {
        CheckoutBrowser.open(uri)
    }
}

fun dismissCheckoutBrowser() {
    CheckoutBrowser.dismiss()
}
