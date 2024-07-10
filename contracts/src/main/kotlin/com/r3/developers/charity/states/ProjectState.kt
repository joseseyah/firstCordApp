package com.r3.developers.charity.states

import com.r3.developers.charity.contracts.ProjectContract
import net.corda.v5.ledger.utxo.BelongsToContract
import net.corda.v5.ledger.utxo.ContractState
import java.security.PublicKey
import java.util.*

/**
 * When a project is CREATED within a charity, the attributes associated are:
 *   - charity (where the project belongs to)
 *   - totalBudget (The budget of the project)
 *   - projectName (Name of the project)
 *   - projectDescription (The description of the project)
 *   - donors (The donors that have donated to the project)
 *   - projectID (the ID related to the project)
 */

@BelongsToContract(ProjectContract::class)
class ProjectState(
    val charity: PublicKey,
    val totalBudget: Int,
    val charityTotalFunds: Int,
    val projectName: String,
    val projectDescription: String,
    val donors: List<PublicKey>,
    val projectID: UUID
) : ContractState {
    override fun getParticipants(): List<PublicKey> = listOf(charity) + donors

    fun donating(donor: PublicKey): ProjectState {
        return ProjectState(charity, totalBudget, charityTotalFunds, projectName, projectDescription, donors + donor, projectID)
    }
}
