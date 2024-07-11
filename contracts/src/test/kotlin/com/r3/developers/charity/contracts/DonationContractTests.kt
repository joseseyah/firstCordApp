package com.r3.developers.charity.contracts

import com.r3.corda.ledger.utxo.testing.ContractTest
import com.r3.corda.ledger.utxo.testing.buildTransaction
import com.r3.developers.charity.states.DonationState
import java.security.KeyPairGenerator
import java.security.PublicKey
import java.util.*
import org.junit.jupiter.api.Test


class DonationContractTests : ContractTest() {

    private val keyGen = KeyPairGenerator.getInstance("RSA").apply { initialize(512) }
    private val donorKey: PublicKey = keyGen.generateKeyPair().public
    private val charityKey: PublicKey = keyGen.generateKeyPair().public

    @Test
    fun `valid output state from a donor`(){
        val amount = 100
        val donationID = UUID.randomUUID()
        val transaction = buildTransaction {
            addCommand(CharityCommands.Donate())
            addOutputStates(DonationState(donorKey,charityKey,donationID, amount = amount, participants = emptyList()))
        }
        assertVerifies(transaction)
    }

    @Test
    fun `test donor cannot donate to themselves`(){
        val amount = 100
        val donationID = UUID.randomUUID()
        val transaction = buildTransaction {
            addCommand(CharityCommands.Donate())
            addOutputStates(DonationState(donorKey,charityKey,donationID, amount = amount, participants = emptyList()))
        }
        if (donorKey == charityKey)
            assertFailsWith(transaction, "Donor cannot donate to itself and try to pose as a charity")

    }

    @Test
    fun `test donation amount is not zero`(){
        val amount = 0
        val donationID = UUID.randomUUID()
        val transaction = buildTransaction {
            addCommand(CharityCommands.Donate())
            addOutputStates(DonationState(donorKey,charityKey,donationID,amount, participants = emptyList()))
        }
        assertFailsWith(transaction,"The amount sent cannot be zero")
    }


}

























//class DonationContractTest {
//
//    private val mockLedgerService = MockLedgerService()
//
//    private val keyGen = KeyPairGenerator.getInstance("RSA").apply { initialize(512) }
//    private val donorKey: PublicKey = keyGen.generateKeyPair().public
//    private val charityKey: PublicKey = keyGen.generateKeyPair().public
//    private val otherKey: PublicKey = keyGen.generateKeyPair().public
//
//    private fun createMockTransaction(
//        command: Command,
//        outputState: DonationState
//    ): UtxoLedgerTransaction {
//        return MockUtxoLedgerTransaction(
//            ledgerService = mockLedgerService,
//            id = UUID.randomUUID(),
//            inputs = emptyList(),
//            outputs = listOf(outputState),
//            commands = listOf(command)
//        )
//    }

//    @Test
//    fun `verify should pass with valid donation state`() {
//        val donationID = UUID.randomUUID()
//        val amount = 100
//        val command = CharityCommands.Donate()
//        val outputState = DonationState(donorKey, charityKey, donationID, amount, listOf(donorKey, charityKey))
//        val transaction = createMockTransaction(command, outputState)
//
//        val contract = DonationContract()
//        contract.verify(transaction)
//    }
//
//    @Test
//    fun `verify should fail if output states size is not one`() {
//        val donationID = UUID.randomUUID()
//        val amount = 100
//        val command = CharityCommands.Donate()
//        val outputState = DonationState(donorKey, charityKey, donationID, amount, listOf(donorKey, charityKey))
//        val transaction = MockUtxoLedgerTransaction(
//            ledgerService = mockLedgerService,
//            id = UUID.randomUUID(),
//            inputs = emptyList(),
//            outputs = listOf(outputState, outputState),
//            commands = listOf(command)
//        )
//
//        val contract = DonationContract()
//        assertThrows(IllegalArgumentException::class.java) {
//            contract.verify(transaction)
//        }
//    }
//
//    @Test
//    fun `verify should fail if amount is not greater than zero`() {
//        val donationID = UUID.randomUUID()
//        val amount = 0
//        val command = CharityCommands.Donate()
//        val outputState = DonationState(donorKey, charityKey, donationID, amount, listOf(donorKey, charityKey))
//        val transaction = createMockTransaction(command, outputState)
//
//        val contract = DonationContract()
//        assertThrows(IllegalArgumentException::class.java) {
//            contract.verify(transaction)
//        }
//    }
//
//    @Test
//    fun `verify should fail if donor and charity are the same`() {
//        val donationID = UUID.randomUUID()
//        val amount = 100
//        val command = CharityCommands.Donate()
//        val outputState = DonationState(donorKey, donorKey, donationID, amount, listOf(donorKey, donorKey))
//        val transaction = createMockTransaction(command, outputState)
//
//        val contract = DonationContract()
//        assertThrows(
























//class DonationContractTest {
//
//    private val mockLedgerService = MockLedgerService()
//
//    private fun createMockTransaction(
//        command: Command,
//        outputState: DonationState
//    ): UtxoLedgerTransaction {
//        return MockUtxoLedgerTransaction(
//            ledgerService = mockLedgerService,
//            id = UUID.randomUUID(),
//            inputs = emptyList(),
//            outputs = listOf(outputState),
//            commands = listOf(command)
//        )
//    }
//
//    @Test
//    fun `verify should pass with valid donation state`() {
//        val donor = "DonorParty"
//        val charity = "CharityParty"
//        val amount = 100
//        val command = CharityCommands.Donate()
//        val outputState = DonationState(amount, donor, charity)
//        val transaction = createMockTransaction(command, outputState)
//
//        val contract = DonationContract()
//        contract.verify(transaction)
//    }
//
//    @Test
//    fun `verify should fail if output states size is not one`() {
//        val donor = "DonorParty"
//        val charity = "CharityParty"
//        val amount = 100
//        val command = CharityCommands.Donate()
//        val outputState = DonationState(amount, donor, charity)
//        val transaction = MockUtxoLedgerTransaction(
//            ledgerService = mockLedgerService,
//            id = UUID.randomUUID(),
//            inputs = emptyList(),
//            outputs = listOf(outputState, outputState),
//            commands = listOf(command)
//        )
//
//        val contract = DonationContract()
//        assertThrows(IllegalArgumentException::class.java) {
//            contract.verify(transaction)
//        }
//    }
//
//    @Test
//    fun `verify should fail if amount is not greater than zero`() {
//        val donor = "DonorParty"
//        val charity = "CharityParty"
//        val amount = 0
//        val command = CharityCommands.Donate()
//        val outputState = DonationState(amount, donor, charity)
//        val transaction = createMockTransaction(command, outputState)
//
//        val contract = DonationContract()
//        assertThrows(IllegalArgumentException::class.java) {
//            contract.verify(transaction)
//        }
//    }


