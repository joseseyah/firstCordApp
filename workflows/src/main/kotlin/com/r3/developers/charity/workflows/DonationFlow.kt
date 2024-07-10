package com.r3.developers.charity.workflows

import com.r3.developers.charity.contracts.CharityCommands
import com.r3.developers.charity.states.DonationState
import net.corda.v5.application.flows.*
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.application.membership.MemberLookup
import net.corda.v5.application.messaging.FlowMessaging
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.types.MemberX500Name
import net.corda.v5.ledger.common.NotaryLookup
import net.corda.v5.ledger.utxo.UtxoLedgerService
import java.time.Instant
import java.util.*

@InitiatingFlow(protocol = "donating-to-charity")
class DonationFlow : ClientStartableFlow {

    internal data class DonationRequest(
        val amount: Int,
        val holder: MemberX500Name,
    )

    @CordaInject
    lateinit var jsonMarshallingService: JsonMarshallingService

    @CordaInject
    lateinit var memberLookup: MemberLookup

    @CordaInject
    lateinit var notaryLookup: NotaryLookup

    @CordaInject
    lateinit var utxoLedgerService: UtxoLedgerService

    @CordaInject
    lateinit var flowMessaging: FlowMessaging

    @Suspendable
    override fun call(requestBody: ClientRequestBody): String {
        // Parse the request
        val request = requestBody.getRequestBodyAs(jsonMarshallingService, DonationRequest::class.java)
        val holderName = request.holder

        // Look up the notary
        val notary = notaryLookup.notaryServices.first()
            ?: throw IllegalArgumentException("Notary not found")

        // Get donor and charity keys
        val donor = memberLookup.myInfo().ledgerKeys.first()
        val holder = memberLookup.lookup(holderName)?.ledgerKeys?.first()
            ?: throw IllegalArgumentException("The charity $holderName does not exist within the network")

        // Create DonationState
        val newDonation = DonationState(
            donationID = UUID.randomUUID(),
            amount = request.amount,
            donor = donor,
            charity = holder,
            participants = listOf(donor, holder)
        )

        // Build the transaction
        val transactionBuilder = utxoLedgerService.createTransactionBuilder()
            .setNotary(notary.name)
            .addOutputState(newDonation)
            .addCommand(CharityCommands.Donate())
            .setTimeWindowBetween(Instant.now().minusSeconds(60), Instant.now().plusSeconds(60))
            .addSignatories(listOf(donor, holder))
            .toSignedTransaction()


        // Initiate flow session with charity
        val session = flowMessaging.initiateFlow(holderName)

        return try {
            // Finalize the transaction
            utxoLedgerService.finalize(transactionBuilder, listOf(session))
            newDonation.donationID.toString()
        } catch (e: Exception) {
            "Flow failed, message: ${e.stackTrace}"
        }
    }
}

/*
RequestBody for triggering the flow via REST:

{
    "clientRequestId": "donate-1",
    "flowClassName": "com.r3.developers.charity.workflows.DonationFlow",
    "requestBody": {
        "amount":100,
        "holder":"CN=WorldCharity, OU=Test Dept, O=R3, L=London, C=GB"
        }
}

*/

