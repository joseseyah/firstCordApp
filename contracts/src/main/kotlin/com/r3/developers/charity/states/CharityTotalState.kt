package com.r3.developers.charity.states

import net.corda.v5.base.types.MemberX500Name
import net.corda.v5.ledger.utxo.ContractState
import java.security.PublicKey

class CharityTotalState (
    val totalAmount : Int,
    val charity: MemberX500Name,
    private val participants: List<PublicKey>
) : ContractState{
    override fun getParticipants(): List<PublicKey> = participants



}