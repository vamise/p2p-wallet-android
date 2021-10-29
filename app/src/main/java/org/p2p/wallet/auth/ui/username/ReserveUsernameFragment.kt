package org.p2p.wallet.auth.ui.username

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.StyleSpan
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.widget.doAfterTextChanged
import com.geetest.sdk.GT3ConfigBean
import com.geetest.sdk.GT3ErrorBean
import com.geetest.sdk.GT3GeetestUtils
import com.geetest.sdk.GT3Listener
import org.json.JSONObject
import org.koin.android.ext.android.inject
import org.p2p.wallet.BuildConfig
import org.p2p.wallet.R
import org.p2p.wallet.auth.ui.pin.create.CreatePinFragment
import org.p2p.wallet.auth.ui.pin.create.PinLaunchMode
import org.p2p.wallet.auth.ui.security.SecurityKeyFragment
import org.p2p.wallet.common.mvp.BaseMvpFragment
import org.p2p.wallet.databinding.FragmentReserveUsernameBinding
import org.p2p.wallet.restore.ui.derivable.DerivableAccountsFragment
import org.p2p.wallet.utils.args
import org.p2p.wallet.utils.colorFromTheme
import org.p2p.wallet.utils.edgetoedge.Edge
import org.p2p.wallet.utils.edgetoedge.edgeToEdge
import org.p2p.wallet.utils.popBackStack
import org.p2p.wallet.utils.replaceFragment
import org.p2p.wallet.utils.toast
import org.p2p.wallet.utils.viewbinding.viewBinding
import org.p2p.wallet.utils.withArgs
import timber.log.Timber
import java.io.File

class ReserveUsernameFragment :
    BaseMvpFragment<ReserveUsernameContract.View,
        ReserveUsernameContract.Presenter>(R.layout.fragment_reserve_username),
    ReserveUsernameContract.View {

    companion object {
        private const val KEY_OPENED_FROM: String = "opened_from"
        fun create(fromClass: String) =
            ReserveUsernameFragment()
                .withArgs(KEY_OPENED_FROM to fromClass)
    }

    override val presenter: ReserveUsernameContract.Presenter by inject()

    private val binding: FragmentReserveUsernameBinding by viewBinding()
    private var gt3GeeTestUtils: GT3GeetestUtils? = null
    private var gt3ConfigBean: GT3ConfigBean? = null

    private val openedFrom: String by args(KEY_OPENED_FROM)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initGeetestUtils()

        binding.run {
            edgeToEdge {
                toolbar.fit { Edge.TopArc }
                termsAndConditionsTextView.fitMargin { Edge.BottomArc }
            }
            toolbar.setNavigationOnClickListener { popBackStack() }

            youCanSkipTextView.text = buildClickableText()
            youCanSkipTextView.movementMethod = LinkMovementMethod.getInstance()
            youCanSkipTextView.highlightColor = Color.TRANSPARENT

            termsAndConditionsTextView.text = buildTermsAndPrivacyText()
            termsAndConditionsTextView.movementMethod = LinkMovementMethod.getInstance()

            usernameEditText.doAfterTextChanged {
                presenter.checkUsername(it.toString().lowercase())
            }

            enterUserNameButton.setOnClickListener {
                gt3GeeTestUtils?.startCustomFlow()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        gt3GeeTestUtils?.destory()
    }

    override fun navigateToPinCode() {
        replaceFragment(CreatePinFragment.create(PinLaunchMode.CREATE))
    }

    override fun showUnavailableName(name: String) {
        binding.usernameTextView.text = buildBoldText(getString(R.string.auth_unavailable_name, name), name.length)
        binding.usernameTextView.setTextColor(colorFromTheme(R.attr.colorAccentWarning))
        binding.enterUserNameButton.isEnabled = false
        binding.enterUserNameButton.text = getString(R.string.auth_enter_your_username)
        setTextColorGrey()
    }

    override fun showAvailableName(name: String) {
        binding.usernameTextView.text = buildBoldText(getString(R.string.auth_available_name, name), name.length)
        binding.usernameTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorGreen))
        binding.enterUserNameButton.isEnabled = true
        binding.enterUserNameButton.text = getString(R.string.auth_reserve)
        setTextColorGrey()
    }

    override fun getCaptchaResult(params: JSONObject) {
        gt3ConfigBean?.api1Json = params
        gt3GeeTestUtils?.getGeetest()
    }

    override fun successCaptcha() {
        gt3GeeTestUtils?.showSuccessDialog()
    }

    override fun failCaptcha() {
        gt3GeeTestUtils?.showFailedDialog()
    }

    override fun successRegisterName() {
        binding.progressView.visibility = View.GONE
        finishNavigation()
    }

    override fun failRegisterName() {
        binding.progressView.visibility = View.GONE
    }

    override fun showFile(file: File) {
        val fromFile = FileProvider.getUriForFile(
            requireContext(),
            BuildConfig.APPLICATION_ID + ".provider",
            file
        )

        val target = Intent(Intent.ACTION_VIEW)
        target.setDataAndType(fromFile, "application/pdf")
        target.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        target.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        try {
            startActivity(target)
        } catch (e: ActivityNotFoundException) {
            Timber.e(e, "Cannot open file")
            toast(R.string.error_opening_file)
        }
    }

    private fun initGeetestUtils() {
        gt3GeeTestUtils = GT3GeetestUtils(requireContext())
        gt3ConfigBean = GT3ConfigBean()
        gt3ConfigBean?.pattern = 1
        gt3ConfigBean?.isCanceledOnTouchOutside = false
        gt3ConfigBean?.lang = null
        gt3ConfigBean?.timeout = 10000
        gt3ConfigBean?.webviewTimeout = 10000
        gt3ConfigBean?.listener = object : GT3Listener() {
            override fun onDialogResult(result: String?) {
                presenter.registerUsername(binding.usernameEditText.text.toString().lowercase(), result)
                gt3GeeTestUtils?.showSuccessDialog()
            }

            override fun onReceiveCaptchaCode(p0: Int) {
            }

            override fun onStatistics(p0: String?) {
            }

            override fun onClosed(p0: Int) {
            }

            override fun onSuccess(p0: String?) {
                binding.progressView.visibility = View.VISIBLE
            }

            override fun onFailed(p0: GT3ErrorBean?) {
            }

            override fun onButtonClick() {
                presenter.checkCaptcha()
            }
        }
        gt3GeeTestUtils?.init(gt3ConfigBean)
    }

    private fun buildTermsAndPrivacyText(): SpannableString {
        val message = getString(R.string.auth_agree_terms_and_privacy)
        val span = SpannableString(message)

        /*
        * Applying clickable span for terms of use
        * */
        val clickableTermsOfUse = object : ClickableSpan() {
            override fun onClick(widget: View) {
                presenter.openTermsOfUse()
            }
        }
        val termsOfUse = getString(R.string.auth_terms_of_use)
        val termsStart = span.indexOf(termsOfUse)
        val termsEnd = span.indexOf(termsOfUse) + termsOfUse.length
        span.setSpan(clickableTermsOfUse, termsStart, termsEnd, Spanned.SPAN_INCLUSIVE_INCLUSIVE)

        val privacyPolicy = getString(R.string.auth_privacy_policy)

        /*
        * Applying clickable span for privacy policy
        * */
        val clickablePrivacy = object : ClickableSpan() {
            override fun onClick(widget: View) {
                presenter.openPrivacyPolicy()
            }
        }
        val start = span.indexOf(privacyPolicy)
        val end = span.indexOf(privacyPolicy) + privacyPolicy.length
        span.setSpan(clickablePrivacy, start, end, Spanned.SPAN_INCLUSIVE_INCLUSIVE)

        return span
    }

    private fun buildClickableText(): SpannableString {
        val clickableText = getString(R.string.auth_clickable_skip_this_step)
        val message = getString(R.string.auth_skip_this_step)
        val span = SpannableString(message)
        val clickableNumber = object : ClickableSpan() {
            override fun onClick(widget: View) {
                finishNavigation()
            }
        }
        val start = span.indexOf(clickableText)
        val end = span.indexOf(clickableText) + clickableText.length
        span.setSpan(clickableNumber, start, end, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
        return span
    }

    private fun finishNavigation() {
        when (openedFrom) {
            SecurityKeyFragment.TAG -> navigateToPinCode()
            DerivableAccountsFragment.TAG -> navigateToPinCode()
            else -> popBackStack()
        }
    }

    private fun buildBoldText(text: String, boldLength: Int): SpannableString {
        val span = SpannableString(text)
        span.setSpan(StyleSpan(Typeface.BOLD), 0, boldLength, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        return span
    }

    private fun setTextColorGrey() {
        if (binding.usernameEditText.text?.isEmpty() == true) {
            binding.usernameTextView.text = getString(R.string.auth_use_any_latin)
            binding.usernameTextView.setTextColor(colorFromTheme(R.attr.colorElementSecondary))
        }
    }
}