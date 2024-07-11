package com.r3.developers.charity.workflows

import com.r3.developers.charity.states.DonationState
import com.r3.developers.charity.states.ProjectState
import net.corda.v5.application.flows.ClientRequestBody
import net.corda.v5.application.flows.ClientStartableFlow
import net.corda.v5.application.flows.CordaInject
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.application.membership.MemberLookup
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.types.MemberX500Name
import net.corda.v5.ledger.utxo.UtxoLedgerService
import org.slf4j.LoggerFactory
import java.time.Instant

data class GetTotal(val charity: MemberX500Name)

class CharityTotal : ClientStartableFlow {

    private companion object {
        val log = LoggerFactory.getLogger(this::class.java.enclosingClass)
    }

    @CordaInject
    lateinit var jsonMarshallingService: JsonMarshallingService

    @CordaInject
    lateinit var ledgerService: UtxoLedgerService

    @CordaInject
    lateinit var memberLookup: MemberLookup

    @Suspendable
    override fun call(requestBody: ClientRequestBody): String {

        // Extract arguments from request body
        val flowArgs = requestBody.getRequestBodyAs(jsonMarshallingService, GetTotal::class.java)

        val charityX500Name = memberLookup.lookup(flowArgs.charity)?.name
            ?: throw IllegalArgumentException("The charity ${flowArgs.charity} does not exist within the network")

        val charityName = memberLookup.lookup(charityX500Name)?.ledgerKeys?.first()
            ?: throw IllegalArgumentException("The charity ${flowArgs.charity} does not have a valid public key")

        log.info("charityName: {}", charityName)


        // Lookup the PublicKey for the given charity MemberX500Name
        val charityMemberInfo = memberLookup.lookup(charityName)
            ?: throw IllegalArgumentException("The charity $charityName does not exist within the network")

        log.info("charityName: {}", charityMemberInfo)

        // Fetch unconsumed DonationState objects
        val states = ledgerService.findUnconsumedStatesByExactType(DonationState::class.java, 100, Instant.now()).results
        log.info("States: {}", states)
        if (states.isEmpty()){
            val totalDonation = 0
            val allocationFunds = 0
            val responseMap = mapOf(
                "totalDonations" to totalDonation,
                "allocationFunds" to allocationFunds,
                "Remaining Funds" to totalDonation - allocationFunds
            )
            return jsonMarshallingService.format(responseMap)
        }
        else{
            // Calculate total donation amount to the specified charity
            val totalDonation = states
                .filter { it.state.contractState.charity == charityMemberInfo.ledgerKeys.first() }
                .sumOf { it.state.contractState.amount }

            val allocationStates = ledgerService.findUnconsumedStatesByExactType(ProjectState::class.java, 100, Instant.now()).results
            if (allocationStates.isEmpty()){
                val allocationFunds = 0
                val responseMap = mapOf(
                    "totalDonations" to totalDonation,
                    "allocationFunds" to allocationFunds,
                    "Remaining Funds" to totalDonation - allocationFunds
                )
                return jsonMarshallingService.format(responseMap)

            }
            else{
                val allocationFunds = allocationStates
                    .filter { it.state.contractState.charity == charityMemberInfo.ledgerKeys.first()}
                    .sumOf { it.state.contractState.totalBudget }

                // Log the total donation
                log.info("Total donation to charity $charityName: $totalDonation")

                // Return the total donation amount as a JSON string
                val responseMap = mapOf(
                    "totalDonations" to totalDonation,
                    "allocationFunds" to allocationFunds,
                    "Remaining Funds" to totalDonation - allocationFunds
                )
                return jsonMarshallingService.format(responseMap)
            }
        }
    }
}

/*
RequestBody for triggering the flow via REST:
{
    "clientRequestId": "get-total",
    "flowClassName": "com.r3.developers.charity.workflows.CharityTotal",
    "requestBody": {
        "charity": "CN=WorldCharity, OU=Test Dept, O=R3, L=London, C=GB"
    }
}
*/
