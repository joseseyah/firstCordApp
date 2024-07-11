package com.r3.developers.charity.workflows

import com.r3.developers.charity.contracts.CharityCommands
import com.r3.developers.charity.states.DonationState
import com.r3.developers.charity.states.ProjectState
import java.security.PublicKey
import net.corda.v5.application.flows.ClientRequestBody
import net.corda.v5.application.flows.ClientStartableFlow
import net.corda.v5.application.flows.CordaInject
import net.corda.v5.application.flows.InitiatingFlow
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.application.membership.MemberLookup
import net.corda.v5.application.messaging.FlowMessaging
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.exceptions.CordaRuntimeException
import net.corda.v5.base.types.MemberX500Name
import net.corda.v5.ledger.common.NotaryLookup
import net.corda.v5.ledger.utxo.UtxoLedgerService
import net.corda.v5.ledger.utxo.transaction.UtxoTransactionBuilder
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

@InitiatingFlow(protocol = "funding-project")
class AllocateFunds : ClientStartableFlow {

    private companion object {
        val log = LoggerFactory.getLogger(this::class.java.enclosingClass)
    }

    internal data class FundsRequest(
        val amount: Int,
        val projectID: UUID,
        val charityName: MemberX500Name
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

    @CordaInject
    lateinit var ledgerService: UtxoLedgerService

    @Suspendable
    override fun call(requestBody: ClientRequestBody): String {
        log.info("AllocateFunds.call() called")

        val request = requestBody.getRequestBodyAs(jsonMarshallingService, FundsRequest::class.java)
        log.info("FundsRequest: {}", request)

        // Retrieve the notary service
        val notary = notaryLookup.notaryServices.single()

        // Lookup the project state by project ID
        val states = utxoLedgerService.findUnconsumedStatesByExactType(ProjectState::class.java, 100, Instant.now()).results
        log.info("States found: {}", states.size)

        val stateAndRef = states.singleOrNull { it.state.contractState.projectID == request.projectID }
            ?: throw CordaRuntimeException("Did not find a unique unconsumed ProjectState with projectID ${request.projectID}")

        val oldState = stateAndRef.state.contractState

        //Check if funds are available in charity------------------------------

        val AllDonationStates = ledgerService.findUnconsumedStatesByExactType(DonationState::class.java, 100, Instant.now()).results
        val AllAllocationStates = ledgerService.findUnconsumedStatesByExactType(ProjectState::class.java, 100, Instant.now()).results
        val charityMember = memberLookup.lookup(request.charityName)
        val totalDonations = AllDonationStates
            .filter {it.state.contractState.charity == charityMember?.ledgerKeys?.first()}
            .sumOf { it.state.contractState.amount }

        val totalAllocations = AllAllocationStates
            .filter { it.state.contractState.charity == charityMember?.ledgerKeys?.first() }
            .sumOf { it.state.contractState.totalBudget }

        val remainder = totalDonations - totalAllocations

        if (remainder < 0){
            throw CordaRuntimeException("Insufficient funds available.")
        }

        //--------------------------------------------------------------------

        val donors = mutableListOf<PublicKey>()
        val donorNames = mutableListOf<MemberX500Name>()
        var allocatedAmount = 0

        for (donationStateAndRef in AllDonationStates){
            val donationState = donationStateAndRef.state.contractState
            if (donationState.charity == charityMember?.ledgerKeys?.first()) {
                allocatedAmount += donationState.amount
                donors.add(donationState.donor)
                val donorMember = memberLookup.lookup(donationState.donor)
                if (donorMember != null) {
                    donorNames.add(donorMember.name)
                }
                if (allocatedAmount >= request.amount) {
                    break
                }
            }
        }

        if (allocatedAmount < request.amount) {
            throw CordaRuntimeException("Unable to find enough donations to cover the requested amount.")
        }



        //--------------------------------------------------------------------



        // Create a new ProjectState with updated budget
        val updatedState = ProjectState(
            charity = oldState.charity,
            totalBudget = oldState.totalBudget + request.amount,
            charityTotalFunds = oldState.charityTotalFunds,
            projectName = oldState.projectName,
            projectDescription = oldState.projectDescription,
            donors = oldState.donors + donors,
            projectID = oldState.projectID
        )

        // Build the transaction
        val transactionBuilder = utxoLedgerService.createTransactionBuilder()
            .setNotary(notary.name)
            .addInputState(stateAndRef.ref)
            .addOutputState(updatedState)
            .addCommand(CharityCommands.Allocate())
            .setTimeWindowUntil(Instant.now().plus(1, ChronoUnit.DAYS))
            .addSignatories(listOf(oldState.charity))

        // Sign the transaction
        val signedTransaction = transactionBuilder.toSignedTransaction()

        // Finalize the transaction
        val session = flowMessaging.initiateFlow(request.charityName)
        return try {
            utxoLedgerService.finalize(signedTransaction, listOf(session))
            log.info("Funds allocated successfully for projectID: {}", request.projectID)
            jsonMarshallingService.format(mapOf("projectID" to request.projectID.toString(),"donors" to donorNames.map { it.toString() }, "allocatedAmount" to request.amount))
        } catch (e: Exception) {
            log.error("Flow failed, message: ${e.message}", e)
            "Flow failed, message: ${e.message}"
        }
    }
}

/*
RequestBody for triggering the flow via REST:
{
    "clientRequestId": "allocate-to-housing",
    "flowClassName": "com.r3.developers.charity.workflows.AllocateFunds",
    "requestBody": {
        "amount" : 100,
        "projectID" : "*",
        "charityName": "CN=WorldCharity, OU=Test Dept, O=R3, L=London, C=GB"
    }
}
 */
