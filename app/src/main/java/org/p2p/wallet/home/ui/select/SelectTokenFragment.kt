package org.p2p.wallet.home.ui.select

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.setFragmentResult
import androidx.recyclerview.widget.LinearLayoutManager
import org.p2p.wallet.R
import org.p2p.wallet.common.mvp.BaseFragment
import org.p2p.wallet.databinding.FragmentSelectTokenBinding
import org.p2p.wallet.home.model.Token
import org.p2p.wallet.utils.args
import org.p2p.wallet.utils.attachAdapter
import org.p2p.wallet.utils.popBackStack
import org.p2p.wallet.utils.viewbinding.viewBinding
import org.p2p.wallet.utils.withArgs

private const val EXTRA_ALL_TOKENS = "EXTRA_ALL_TOKENS"
private const val EXTRA_REQUEST_KEY = "EXTRA_REQUEST_KEY"
private const val EXTRA_RESULT_KEY = "EXTRA_RESULT_KEY"

class SelectTokenFragment(
    private val onSelected: ((Token) -> Unit)?
) : BaseFragment(R.layout.fragment_select_token) {

    companion object {

        fun create(tokens: List<Token>, requestKey: String, resultKey: String) = SelectTokenFragment(null)
            .withArgs(
                EXTRA_ALL_TOKENS to tokens,
                EXTRA_REQUEST_KEY to requestKey,
                EXTRA_RESULT_KEY to resultKey
            )
    }

    private val tokens: List<Token> by args(EXTRA_ALL_TOKENS)
    private val resultKey: String by args(EXTRA_RESULT_KEY)
    private val requestKey: String by args(EXTRA_REQUEST_KEY)

    private val binding: FragmentSelectTokenBinding by viewBinding()

    private val tokenAdapter: SelectTokenAdapter by lazy {
        SelectTokenAdapter {
            onSelected?.invoke(it)
            setFragmentResult(requestKey, bundleOf(resultKey to it))
            parentFragmentManager.popBackStack()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding) {
            toolbar.setNavigationOnClickListener { popBackStack() }
            tokenRecyclerView.layoutManager = LinearLayoutManager(requireContext())
            tokenRecyclerView.attachAdapter(tokenAdapter)
            tokenAdapter.setItems(tokens)

            val isEmpty = tokens.isEmpty()
            tokenRecyclerView.isVisible = !isEmpty
            emptyTextView.isVisible = isEmpty
        }
    }
}