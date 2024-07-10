package com.r3.developers.charity.workflows

import com.r3.developers.charity.contracts.CharityCommands
import com.r3.developers.charity.states.ProjectState
import net.corda.v5.application.flows.*
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.application.membership.MemberLookup
import net.corda.v5.application.messaging.FlowMessaging
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.types.MemberX500Name
import net.corda.v5.ledger.common.NotaryLookup
import net.corda.v5.ledger.utxo.UtxoLedgerService
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.math.log

@InitiatingFlow(protocol = "create-project")
class ProjectCreationFlow : ClientStartableFlow {

    internal data class ProjectRequest(
        val totalBudget: Int,
        val projectName: String,
        val projectDescription: String,
        val charityName: MemberX500Name,
        val notary: MemberX500Name
    )

    @CordaInject
    lateinit var jsonMarshallingService: JsonMarshallingService

    @CordaInject
    lateinit var memberLookup: MemberLookup

    @CordaInject
    lateinit var flowMessaging: FlowMessaging

    @CordaInject
    lateinit var notaryLookup: NotaryLookup

    @CordaInject
    lateinit var utxoLedgerService: UtxoLedgerService

    @Suspendable
    override fun call(requestBody: ClientRequestBody): String {
        val request = requestBody.getRequestBodyAs(jsonMarshallingService, ProjectRequest::class.java)

        val charityName = memberLookup.lookup(request.charityName)?.name
            ?: throw IllegalArgumentException("The charity ${request.charityName} does not exist within the network")

        val charity = memberLookup.lookup(charityName)?.ledgerKeys?.first()
            ?: throw IllegalArgumentException("The charity ${request.charityName} does not have a valid public key")

        val notary = notaryLookup.lookup(request.notary)
            ?: throw IllegalArgumentException("Notary ${request.notary} not found")

        val charityTotalFunds = CharityTotal()

        // Create the ProjectState
        val newProject = ProjectState(
            charity = charity,
            totalBudget = 0,
            charityTotalFunds = 0,
            projectName = request.projectName,
            projectDescription = request.projectDescription,
            donors = emptyList(),
            projectID = UUID.randomUUID(),
        )


        // Build the transaction
        val transactionBuilder = utxoLedgerService.createTransactionBuilder()
            .setNotary(notary.name)
            .addOutputState(newProject)
            .addCommand(CharityCommands.Create())
            .setTimeWindowUntil(Instant.now().plus(1, ChronoUnit.DAYS))
            .addSignatories(listOf(charity))
            .toSignedTransaction()

        val session = flowMessaging.initiateFlow(request.charityName)
        // Finalize the transaction
        return try {
            utxoLedgerService.finalize(transactionBuilder, listOf(session))
            newProject.projectID.toString()
        } catch (e: Exception) {
            "Flow failed, message: ${e.message}"
        }
    }
}

/*
RequestBody for triggering the flow via REST:

{
    "clientRequestId": "create-project-1",
    "flowClassName": "com.r3.developers.charity.workflows.ProjectCreationFlow",
    "requestBody": {
        "totalBudget": 0,
        "projectName": "Housing",
        "projectDescription": "Building homes",
        "charityName": "CN=WorldCharity, OU=Test Dept, O=R3, L=London, C=GB",
        "notary": "CN=NotaryService, OU=Test Dept, O=R3, L=London, C=GB"
    }
}


 */
