package com.r3.developers.charity.contracts

import com.r3.corda.ledger.utxo.testing.ContractTest
import com.r3.corda.ledger.utxo.testing.buildTransaction
import com.r3.developers.charity.states.ProjectState
import java.security.KeyPairGenerator
import java.security.PublicKey
import java.util.*
import org.junit.jupiter.api.Test

class ProjectContractTests : ContractTest() {

    private val keyGen = KeyPairGenerator.getInstance("RSA").apply { initialize(512) }
    private val charityKey: PublicKey = keyGen.generateKeyPair().public

    @Test
    fun `test that project name is not empty`(){
        val transaction = buildTransaction {
            addCommand(CharityCommands.Create())
            addOutputStates(ProjectState(charityKey,0, 0, "","description", emptyList(),UUID.randomUUID()))
        }
        assertFailsWith(transaction, "The project name cannot be blank")
    }

    @Test
    fun `test that project description not empty`(){
        val transaction = buildTransaction {
            addCommand(CharityCommands.Create())
            addOutputStates(ProjectState(charityKey,0, 0, "Name","", emptyList(),UUID.randomUUID()))
        }
        assertFailsWith(transaction,"The project Description cannot be blank")

    }


}