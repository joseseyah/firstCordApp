package com.r3.developers.charity.contracts

import net.corda.v5.ledger.utxo.Command

interface CharityCommands : Command {

    //Donate: Donor is donating to a charity
    class Donate: CharityCommands

    //Create: Creating the project
    class Create: CharityCommands

    //Allocate: allocates the funds to a project
    class Allocate: CharityCommands

    class AddDonation: CharityCommands
}