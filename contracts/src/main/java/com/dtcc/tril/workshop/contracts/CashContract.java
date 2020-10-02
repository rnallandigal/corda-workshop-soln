package com.dtcc.tril.workshop.contracts;

import com.dtcc.tril.workshop.states.Cash;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.CommandWithParties;
import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;

import static net.corda.core.contracts.ContractsDSL.requireSingleCommand;
import static net.corda.core.contracts.ContractsDSL.requireThat;

// ************
// * Contract *
// ************
public class CashContract implements Contract {
    // This is used to identify our contract when building a transaction.
    public static final String ID = "com.dtcc.tril.workshop.contracts";

    // A transaction is valid if the verify() function of the contract of all the transaction's input and output states
    // does not throw an exception.
    @Override
    public void verify(LedgerTransaction tx) {

        /* We can use the requireSingleCommand function to extract command data from transaction.
         * However, it is possible to have multiple commands in a signle transaction.*/
        final CommandWithParties<Commands> command = requireSingleCommand(tx.getCommands(), Commands.class);
        final Commands commandData = command.getValue();

        if (commandData.equals(new Commands.IssueCash())) {
            //Retrieve the output state of the transaction
            Cash output = tx.outputsOfType(Cash.class).get(0);

            //Using Corda DSL function requireThat to replicate conditions-checks
            requireThat(require -> {
                require.using("No inputs should be consumed when issuing Cash.", tx.getInputStates().size() == 0);
                require.using("The currency must be USD", output.getCurrency().equals("USD"));
                require.using("The amount must be greater than 0", output.getAmount() > 0);
                require.using("The amount must be less than 500,000", output.getAmount() < 500000);
                return null;
            });
        } 
    }

    // Used to indicate the transaction's intent.
    public interface Commands extends CommandData {
        class IssueCash implements Commands {}
    }
}