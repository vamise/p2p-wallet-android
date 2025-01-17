package org.p2p.wallet.common.ui.textwatcher

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import org.p2p.wallet.R
import org.p2p.wallet.utils.Constants
import java.lang.ref.WeakReference
import kotlin.properties.Delegates

class PrefixTextWatcher(
    editText: EditText,
    private val prefixSymbol: String,
    private val onValueChanged: ((PrefixData) -> Unit)? = null
) : TextWatcher {

    companion object {
        fun installOn(
            editText: EditText,
            prefixSymbol: String = Constants.USD_SYMBOL,
            onValueChanged: ((PrefixData) -> Unit)? = null
        ): PrefixTextWatcher {
            val prefixWatcher = PrefixTextWatcher(editText, prefixSymbol, onValueChanged)
            editText.addTextChangedListener(prefixWatcher)
            editText.setTag(R.id.prefix_watcher_tag_id, prefixWatcher)
            return prefixWatcher
        }

        fun uninstallFrom(editText: EditText) {
            val prefixWatcher = editText.getTag(R.id.prefix_watcher_tag_id) as? PrefixTextWatcher
            editText.removeTextChangedListener(prefixWatcher)
        }
    }

    private val field = WeakReference(editText)

    private var valueText: PrefixData by Delegates.observable(PrefixData()) { _, oldValue, newValue ->
        if (oldValue != newValue) onValueChanged?.invoke(newValue)
    }

    override fun beforeTextChanged(text: CharSequence?, start: Int, count: Int, after: Int) = Unit

    override fun onTextChanged(text: CharSequence?, start: Int, before: Int, count: Int) {
        val clearedValue = text.toString().removePrefix(prefixSymbol).replace(',', '.')

        if (clearedValue.isEmpty()) {
            valueText = PrefixData()
            return
        }

        valueText = PrefixData(
            prefixText = "$prefixSymbol$clearedValue",
            valueWithoutPrefix = clearedValue
        )
    }

    override fun afterTextChanged(edit: Editable?) {
        field.get()?.apply {
            removeTextChangedListener(this@PrefixTextWatcher)
            edit?.clear()
            edit?.append(valueText.prefixText)
            addTextChangedListener(this@PrefixTextWatcher)
        }
    }
}