package com.dtcc.tril.workshop.states;

import com.dtcc.tril.workshop.contracts.AssetContract;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.identity.Party;

// *********
// * State *
// *********
@BelongsToContract(AssetContract.class)
public class Stock extends Asset {

    private String ticker;

    /* Constructor of your Corda state */
    public Stock(String ticker, double amount, Party owner) {
        super(amount, owner);
        this.ticker = ticker;
    }

	public String getTicker() { return ticker; }
}