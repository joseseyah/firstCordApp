package com.r3.developers.charity.workflows

import com.r3.developers.charity.states.DonationState
import net.corda.v5.application.flows.ClientRequestBody
import net.corda.v5.application.flows.ClientStartableFlow
import net.corda.v5.application.flows.CordaInject
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.exceptions.CordaRuntimeException
import net.corda.v5.ledger.utxo.UtxoLedgerService
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*

data class GetDonationFlowArgs(val donationID: UUID)

class GetDonation: ClientStartableFlow {

    private companion object {
        val log = LoggerFactory.getLogger(this::class.java.enclosingClass)
    }

    @CordaInject
    lateinit var jsonMarshallingService: JsonMarshallingService

    @CordaInject
    lateinit var ledgerService: UtxoLedgerService

    @Suspendable
    override fun call(requestBody: ClientRequestBody): String {

        log.info("GetDonation.call() called")

        // Obtain the deserialized input arguments to the flow from the requestBody.
        val flowArgs = requestBody.getRequestBodyAs(jsonMarshallingService, GetDonationFlowArgs::class.java)
        log.info("Flow arguments : {}", flowArgs)

        val states = ledgerService.findUnconsumedStatesByExactType(DonationState::class.java, 100, Instant.now()).results

        log.info("States: {}", states)

        val state = states.singleOrNull { it.state.contractState.donationID == flowArgs.donationID }
            ?: throw CordaRuntimeException("Did not find an unique unconsumed DonationState with donationID ${flowArgs.donationID}")

        log.info("State: {}", state)

        val amount = state.state.contractState.amount
        log.info("Amount: {}", amount)

        return jsonMarshallingService.format(mapOf("amount " to amount))
    }


}

/*
RequestBody for triggering the flow via REST:
{
    "clientRequestId": "get-individual-donation",
    "flowClassName": "com.r3.developers.charity.workflows.GetDonation",
    "requestBody": {
        "donationID": "flow result"
    }
}
*/
