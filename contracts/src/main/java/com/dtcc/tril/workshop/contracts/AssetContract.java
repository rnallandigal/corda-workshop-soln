package com.dtcc.tril.workshop.contracts;

import com.dtcc.tril.workshop.states.Asset;
import com.dtcc.tril.workshop.states.Cash;
import com.dtcc.tril.workshop.states.Stock;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.CommandWithParties;
import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;

import static net.corda.core.contracts.ContractsDSL.requireSingleCommand;
import static net.corda.core.contracts.ContractsDSL.requireThat;

// ************
// * Contract *
// ************
public class AssetContract implements Contract {
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
        } else if (commandData.equals(new Commands.IssueStock())) {
            //Retrieve the output state of the transaction
            Stock output = tx.outputsOfType(Stock.class).get(0);

            //Using Corda DSL function requireThat to replicate conditions-checks
            requireThat(require -> {
                require.using("No inputs should be consumed when issuing Stock.", tx.getInputStates().size() == 0);
                require.using("The ticker must be at most 4 letters", output.getTicker().length() <= 4);
                require.using("The amount must be greater than 0", output.getAmount() > 0);
                require.using("The amount must be less than 500,000", output.getAmount() < 500000);
                return null;
            });
        } else if (commandData.equals(new Commands.TransferCash())) {
            //Retrieve the input state of the transaction
            Cash cashInput = tx.inputsOfType(Cash.class).get(0);

            //Retrieve the output state of the transaction
            Cash cashOutput = tx.outputsOfType(Cash.class).get(0);

            //Using Corda DSL function requireThat to replicate conditions-checks
            requireThat(require -> {
                require.using("The cash input amount must be greater than 0", cashInput.getAmount() > 0);
                require.using("The cash input amount must be less than 500,000", cashInput.getAmount() < 500000);
                require.using("The cash input amount must match the output amount", cashInput.getAmount() == cashOutput.getAmount());
                require.using("The cash input currency must be USD", cashInput.getCurrency().equals("USD"));
                require.using("The cash input currency must match the output currency", cashInput.getAmount() == cashOutput.getAmount());
                require.using("The cash must change owners", !cashInput.getOwner().equals(cashOutput.getOwner()));
                return null;
            });
        } else if (commandData.equals(new Commands.TransferStock())) {
            //Retrieve the input state of the transaction
            Stock stockInput = tx.inputsOfType(Stock.class).get(0);

            //Retrieve the output state of the transaction
            Stock stockOutput = tx.outputsOfType(Stock.class).get(0);

            //Using Corda DSL function requireThat to replicate conditions-checks
            requireThat(require -> {
                require.using("The stock input amount must be greater than 0", stockInput.getAmount() > 0);
                require.using("The stock input amount must be less than 500,000", stockInput.getAmount() < 500000);
                require.using("The stock input amount must match the output amount", stockInput.getAmount() == stockOutput.getAmount());
                require.using("The stock input ticker must be at most 4 letters", stockInput.getTicker().length() <= 4);
                require.using("The stock input currency must match the output currency", stockInput.getAmount() == stockOutput.getAmount());
                require.using("The stock must change owners", !stockInput.getOwner().equals(stockOutput.getOwner()));
                return null;
            });
        } else if (commandData.equals(new Commands.Transfer())) {
            //Retrieve the input state of the transaction
            Cash cashInput = tx.inputsOfType(Cash.class).get(0);
            Stock stockInput = tx.inputsOfType(Stock.class).get(0);

            //Retrieve the output state of the transaction
            Cash cashOutput = tx.outputsOfType(Cash.class).get(0);
            Stock stockOutput = tx.outputsOfType(Stock.class).get(0);

            //Using Corda DSL function requireThat to replicate conditions-checks
            requireThat(require -> {
                require.using("The cash input amount must be greater than 0", cashInput.getAmount() > 0);
                require.using("The cash input amount must be less than 500,000", cashInput.getAmount() < 500000);
                require.using("The cash input amount must match the output amount", cashInput.getAmount() == cashOutput.getAmount());
                require.using("The cash input currency must be USD", cashInput.getCurrency().equals("USD"));
                require.using("The cash input currency must match the output currency", cashInput.getAmount() == cashOutput.getAmount());

                require.using("The stock input amount must be greater than 0", stockInput.getAmount() > 0);
                require.using("The stock input amount must be less than 500,000", stockInput.getAmount() < 500000);
                require.using("The stock input amount must match the output amount", stockInput.getAmount() == stockOutput.getAmount());
                require.using("The stock input ticker must be at most 4 letters", stockInput.getTicker().length() <= 4);
                require.using("The stock input ticker must match the output ticker", stockInput.getTicker() == stockOutput.getTicker());

                require.using("The cash must change owners", !cashInput.getOwner().equals(cashOutput.getOwner()));
                require.using("The stock must change owners", !stockInput.getOwner().equals(stockOutput.getOwner()));
                require.using("The cash input owner must be the stock output owner", !cashInput.getOwner().equals(stockOutput.getOwner()));
                require.using("The stock input owner must be the cash output owner", !stockInput.getOwner().equals(cashOutput.getOwner()));
                return null;
            });
        }
    }

    // Used to indicate the transaction's intent.
    public interface Commands extends CommandData {
        class IssueCash implements Commands {}
        class IssueStock implements Commands {}
        class TransferCash implements Commands {}
        class TransferStock implements Commands {}
        class Transfer implements Commands {}
    }
}