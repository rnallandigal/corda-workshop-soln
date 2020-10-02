package com.dtcc.tril.workshop.states;

import com.dtcc.tril.workshop.contracts.AssetContract;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.ContractState;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;

import java.util.ArrayList;
import java.util.List;

// *********
// * State *
// *********
@BelongsToContract(AssetContract.class)
public class Asset implements ContractState {

    private List<AbstractParty> participants;
    private double amount;
    private Party owner;

    /* Constructor of your Corda state */
    public Asset(double amount, Party owner) {
        this.amount = amount;
        this.owner = owner;
        this.participants = new ArrayList<>();
        this.participants.add(owner);
    }

    public double getAmount() { return amount; }
    public Party getOwner() { return owner; }

    /* This method will indicate who are the participants and required signers when
     * this state is used in a transaction. */
    @Override
    public List<AbstractParty> getParticipants() { return this.participants; }

    public void addParticipant(AbstractParty party) {
        this.participants.add(party);
    }
}