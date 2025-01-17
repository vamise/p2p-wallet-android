package org.p2p.wallet.send.interactor

import org.p2p.wallet.auth.interactor.UsernameInteractor
import org.p2p.wallet.send.model.SearchResult
import org.p2p.wallet.user.interactor.UserInteractor

class SearchInteractor(
    private val usernameInteractor: UsernameInteractor,
    private val userInteractor: UserInteractor
) {

    suspend fun searchByName(username: String): List<SearchResult> {
        val usernames = usernameInteractor.resolveUsername(username)
        return usernames.map { SearchResult.Full(it.owner, it.name) }
    }

    suspend fun searchByAddress(address: String): List<SearchResult> {
        val hasEmptyBalance = userInteractor.getBalance(address.trim()) == 0L
        val result = if (hasEmptyBalance) {
            SearchResult.EmptyBalance(address)
        } else {
            SearchResult.AddressOnly(address)
        }
        return listOf(result)
    }
}