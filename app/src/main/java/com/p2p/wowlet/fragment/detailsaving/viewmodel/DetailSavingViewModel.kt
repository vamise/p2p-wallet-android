package com.p2p.wowlet.fragment.detailsaving.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.p2p.wowlet.R
import com.p2p.wowlet.appbase.viewcommand.Command
import com.p2p.wowlet.appbase.viewmodel.BaseViewModel
import com.wowlet.entities.local.ActivityItem

class DetailSavingViewModel : BaseViewModel() {

    private val listActivityData = mutableListOf(
        ActivityItem("Receive Tokens", "11 oct 2020", "+ 44,51 US$", "0,00344 Tkns"),
        ActivityItem("Send Tokens", "11 oct 2020", "- 44,51 US$", "0,00344 Tkns"),
        ActivityItem("Receive Tokens", "11 oct 2020", "+ 44,51 US$", "0,00344 Tkns"),
        ActivityItem("Receive Tokens", "11 oct 2020", "+ 44,51 US$", "0,00344 Tkns")
    )

    private val _getActivityData by lazy { MutableLiveData<MutableList<ActivityItem>>() }
    val getActivityData: LiveData<MutableList<ActivityItem>> get() = _getActivityData

    init {
        _getActivityData.value = listActivityData
    }

    fun navigateUp() {
        _command.value =
            Command.NavigateUpViewCommand(R.id.action_navigation_detail_saving_to_navigation_dashboard)
    }

}