package com.r3.developers.charity.workflows

import com.r3.developers.charity.states.ProjectState
import net.corda.v5.application.flows.ClientRequestBody
import net.corda.v5.application.flows.ClientStartableFlow
import net.corda.v5.application.flows.CordaInject
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.application.membership.MemberLookup
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.exceptions.CordaRuntimeException
import net.corda.v5.ledger.utxo.UtxoLedgerService
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*

data class GetProjectDetailsFlowArgs(val projectID: UUID)

class GetProjectDetailsFlow : ClientStartableFlow {

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
        log.info("GetProjectDetails.call() called")

        val flowArgs = requestBody.getRequestBodyAs(jsonMarshallingService, GetProjectDetailsFlowArgs::class.java)
        log.info("flowArgs: {}", flowArgs)

        val states = ledgerService.findUnconsumedStatesByExactType(ProjectState::class.java, 100, Instant.now()).results
        log.info("States: {}", states)

        val state = states.singleOrNull { it.state.contractState.projectID == flowArgs.projectID }
            ?: throw CordaRuntimeException("Did not find a unique unconsumed ProjectState with projectID ${flowArgs.projectID}")

        val projectState = state.state.contractState

        // Convert the donors' public keys to MemberX500Name or readable format
        val donors = projectState.donors.mapNotNull { donorKey ->
            memberLookup.lookup(donorKey)?.name
        }

        val responseMap = mapOf(
            "projectName" to projectState.projectName,
            "projectDescription" to projectState.projectDescription,
            "totalBudget" to projectState.totalBudget,
            "donors" to donors.map { it.toString() } // Convert MemberX500Name to String for the response
        )

        return jsonMarshallingService.format(responseMap)
    }
}
/*
{
    "clientRequestId": "get-create-1",
    "flowClassName": "com.r3.developers.charity.workflows.GetProjectDetailsFlow",
    "requestBody": {
        "projectID": "****"
    }
}
*/
