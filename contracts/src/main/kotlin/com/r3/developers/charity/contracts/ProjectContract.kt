package com.r3.developers.charity.contracts
import com.r3.developers.charity.states.ProjectState
import net.corda.v5.ledger.utxo.Contract
import net.corda.v5.ledger.utxo.transaction.UtxoLedgerTransaction

class ProjectContract : Contract {

    override fun verify(transaction: UtxoLedgerTransaction) {
        // Extract the command from the transaction
        when (val command = transaction.commands.first()) {
            is CharityCommands.Create -> {
                // Retrieve the output state of the transaction
                val output = transaction.getOutputStates(ProjectState::class.java).first()
                require(transaction.outputContractStates.size == 1) {
                    "This transaction should only output one state"
                }
                require(output.projectName.isNotBlank()){
                    "The project name cannot be blank"
                }
                require(output.projectDescription.isNotBlank()){
                    "The project Description cannot be blank"
                }


            }

            is CharityCommands.Allocate ->{
                val input = transaction.getInputStates(ProjectState::class.java).first()
                val output = transaction.getOutputStates(ProjectState::class.java).first()
                require(output.totalBudget > input.totalBudget){
                    "The total output must be larger than the input"
                }
                require(output.donors.isNotEmpty()){
                    "The allocation needs to be attached with a donor"
                }

            }

            else -> {
                throw IllegalArgumentException("Incorrect type of BasketOfApples commands: ${command::class.java.name}")
            }
        }
    }
}