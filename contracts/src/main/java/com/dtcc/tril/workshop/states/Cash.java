package com.dtcc.tril.workshop.states;

import com.dtcc.tril.workshop.contracts.AssetContract;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.identity.Party;

// *********
// * State *
// *********
@BelongsToContract(AssetContract.class)
public class Cash extends Asset {

    private String currency;

    /* Constructor of your Corda state */
    public Cash(String currency, double amount, Party owner) {
        super(amount, owner);
        this.currency = currency;
    }

    public String getCurrency() { return currency; }
}