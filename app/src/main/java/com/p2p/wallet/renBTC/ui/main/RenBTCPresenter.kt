package com.p2p.wallet.renBTC.ui.main

import android.content.Context
import android.graphics.Bitmap
import android.os.CountDownTimer
import com.p2p.wallet.common.mvp.BasePresenter
import com.p2p.wallet.qr.interactor.QrCodeInteractor
import com.p2p.wallet.renBTC.interactor.RenBtcInteractor
import com.p2p.wallet.renBTC.service.RenVMService
import com.p2p.wallet.utils.fromLamports
import com.p2p.wallet.utils.scaleMedium
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.p2p.solanaj.kits.renBridge.LockAndMint
import timber.log.Timber
import java.math.BigDecimal
import java.util.concurrent.CancellationException
import java.util.concurrent.TimeUnit

private const val DELAY_IN_MILLIS = 200L
private const val ONE_SECOND_IN_MILLIS = 1000L
private const val THREE_SECONDS = 3500L
private const val BTC_DECIMALS = 8

class RenBTCPresenter(
    private val interactor: RenBtcInteractor,
    private val qrCodeInteractor: QrCodeInteractor
) : BasePresenter<RenBTCContract.View>(), RenBTCContract.Presenter {

    private var sessionTimer: CountDownTimer? = null

    private var qrJob: Job? = null

    private var qrBitmap: Bitmap? = null

    override fun subscribe() {
        launch {
            interactor.getSessionFlow().collect { session ->
                handleSession(session)
            }
        }

        launch {
            interactor.getRenVMStatusFlow().collect { statuses ->
                view?.showLatestStatus(statuses.lastOrNull())
            }
        }
    }

    private fun handleSession(session: LockAndMint.Session?) {
        if (session != null && session.isValid) {
            val remaining = session.expiryTime - System.currentTimeMillis()
            val fee = session.fee.fromLamports(BTC_DECIMALS).multiply(BigDecimal(2)).scaleMedium()
            view?.showActiveState(session.gatewayAddress, remaining.toDateString(), fee.toString())

            startTimer(remaining)
            generateQrCode(session.gatewayAddress)
        } else {
            view?.showIdleState()
        }
    }

    override fun startNewSession(context: Context) {
        launch {
            view?.showLoading(true)
            RenVMService.startWithNewSession(context)
            delay(THREE_SECONDS)
            view?.showLoading(false)
        }
    }

    override fun checkActiveSession(context: Context) {
        launch {
            view?.showLoading(true)
            RenVMService.startWithCheck(context)
            delay(ONE_SECOND_IN_MILLIS)
            view?.showLoading(false)
        }
    }

    private fun generateQrCode(address: String) {
        if (qrJob?.isActive == true) return

        qrJob = launch {
            try {
                delay(DELAY_IN_MILLIS)
                val qr = qrCodeInteractor.generateQrCode(address)
                qrBitmap?.recycle()
                qrBitmap = qr
                view?.renderQr(qr)
            } catch (e: CancellationException) {
                Timber.d("RenBTC qr generation was cancelled")
            } catch (e: Throwable) {
                Timber.e(e, "Failed to generate qr bitmap")
                view?.showErrorMessage()
            }
        }
    }

    private fun startTimer(time: Long) {
        if (sessionTimer != null) return

        sessionTimer = object : CountDownTimer(time, ONE_SECOND_IN_MILLIS) {
            override fun onTick(millisUntilFinished: Long) {
                val timeRemaining = millisUntilFinished.toDateString()
                view?.updateTimer(timeRemaining)
            }

            override fun onFinish() {
                view?.showIdleState()
            }
        }

        sessionTimer!!.start()
    }

    override fun cancelTimer() {
        sessionTimer?.cancel()
        sessionTimer = null
    }

    private fun Long.toDateString(): String {
        val oneDay = TimeUnit.DAYS.toMillis(1)
        var millis = this - oneDay
        val hours = TimeUnit.MILLISECONDS.toHours(millis)
        millis -= TimeUnit.HOURS.toMillis(hours)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
        millis -= TimeUnit.MINUTES.toMillis(minutes)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis)
        return "$hours:$minutes:$seconds"
    }
}