package com.r3.developers.charity.workflows

import com.r3.developers.charity.states.ProjectState
import net.corda.v5.application.flows.ClientRequestBody
import net.corda.v5.application.flows.ClientStartableFlow
import net.corda.v5.application.flows.CordaInject
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.exceptions.CordaRuntimeException
import net.corda.v5.ledger.utxo.UtxoLedgerService
import org.slf4j.LoggerFactory
import java.time.Instant

data class GetProjectIDFlowArgs(val projectName: String)

class GetProjectIDGivenProjectName : ClientStartableFlow {

    private companion object {
        val log = LoggerFactory.getLogger(this::class.java.enclosingClass)
    }

    @CordaInject
    lateinit var jsonMarshallingService: JsonMarshallingService

    @CordaInject
    lateinit var ledgerService: UtxoLedgerService

    @Suspendable
    override fun call(requestBody: ClientRequestBody): String {
        log.info("GetProjectIDGivenProjectName.call() called")

        val flowArgs = requestBody.getRequestBodyAs(jsonMarshallingService, GetProjectIDFlowArgs::class.java)
        log.info("flowArgs: {}", flowArgs)

        val states = ledgerService.findUnconsumedStatesByExactType(ProjectState::class.java, 100, Instant.now()).results
        log.info("States found: {}", states.size)

        val state = states.singleOrNull { it.state.contractState.projectName == flowArgs.projectName }
            ?: throw CordaRuntimeException("Did not find a unique unconsumed ProjectState with project name ${flowArgs.projectName}")

        val projectID = state.state.contractState.projectID
        log.info("Project ID found: {}", projectID)

        return jsonMarshallingService.format(mapOf("projectID" to projectID.toString()))
    }
}

/*
RequestBody for triggering the flow via REST:

{
    "clientRequestId": "get-ID-name",
    "flowClassName": "com.r3.developers.charity.workflows.GetProjectIDGivenProjectName",
    "requestBody": {
        "projectName": "Housing"
    }
}
 */
