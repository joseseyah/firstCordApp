package com.r3.developers.charity.states

import com.r3.developers.charity.contracts.DonationContract
import net.corda.v5.base.types.MemberX500Name
import net.corda.v5.ledger.utxo.BelongsToContract
import net.corda.v5.ledger.utxo.ContractState
import java.security.PublicKey
import java.util.*


/*
* When A donor DONATES to a charity the attributes taken are:
*   - donor (public key)
*   - charity (public key)
*   - donationID (to show that the donor has indeed donated to the charity)
*   - amount(amount donated)
*
* */
@BelongsToContract(DonationContract::class)
class DonationState(
    val donor: PublicKey,
    val charity: PublicKey,
    val donationID: UUID,
    val amount: Int,
    private val participants: List<PublicKey>
) : ContractState{
    override fun getParticipants(): List<PublicKey> = participants
}