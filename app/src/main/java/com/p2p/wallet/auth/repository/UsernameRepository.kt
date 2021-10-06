package com.p2p.wallet.auth.repository

import com.p2p.wallet.auth.api.UsernameCheckResponse
import com.p2p.wallet.auth.model.NameRegisterBody

interface UsernameRepository {
    suspend fun checkUsername(username: String): UsernameCheckResponse
    suspend fun registerUsername(body: NameRegisterBody): String
}