package com.r3.developers.charity.contracts

import com.r3.developers.charity.states.DonationState
import net.corda.v5.ledger.utxo.Contract
import net.corda.v5.ledger.utxo.transaction.UtxoLedgerTransaction

class DonationContract : Contract {
    override fun verify(transaction: UtxoLedgerTransaction) {
        // Extract the command from the transaction
        // Verify the transaction according to the intention of the transaction
        when (val command = transaction.commands.first()) {
            is CharityCommands.Donate -> {
                val output = transaction.getOutputStates(DonationState::class.java).first()
                require(transaction.outputContractStates.size == 1) {
                    "This transaction should only have one donation state as output"
                }
                require(output.amount > 0){
                    "The amount sent should be greater than 0"
                }
                require(output.donor != output.charity){
                    "Donor cannot donate to itself and try to pose as a charity"
                }


            }
            else -> {
                throw IllegalArgumentException("Incorrect type of Donation commands: ${command::class.java.name}")
            }
        }
    }
}