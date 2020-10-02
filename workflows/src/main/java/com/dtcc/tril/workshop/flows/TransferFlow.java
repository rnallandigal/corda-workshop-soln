package com.dtcc.tril.workshop.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.dtcc.tril.workshop.contracts.AssetContract;
import com.dtcc.tril.workshop.states.Cash;
import com.dtcc.tril.workshop.states.Stock;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TransferFlow {
    // ******************
    // * Initiator flow *
    // ******************
    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends FlowLogic<SignedTransaction> {

        // We will not use these ProgressTracker for this Hello-World sample
        private final ProgressTracker progressTracker = new ProgressTracker();

        @Override
        public ProgressTracker getProgressTracker() {
            return progressTracker;
        }

        // private variables
        private final Party party;
        private final Party counterparty;
        private final Cash cash;
        private final Stock stock;

        // public constructor
        public Initiator(Cash cash, Stock stock) {
            this.party = cash.getOwner();
            this.counterparty = stock.getOwner();
            this.cash = cash;
            this.stock = stock;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            // Step 1. Get a reference to the notary service on our network and our key pair.
            final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

            // Step 2. Get the input and output states of the transaction
            final StateAndRef<Cash> input1 = queryCash(cash);
            final StateAndRef<Stock> input2 = queryStock(stock);

            final Cash output1 = new Cash(cash.getCurrency(), cash.getAmount(), stock.getOwner());
            final Stock output2 = new Stock(stock.getTicker(), stock.getAmount(), cash.getOwner());

            output1.addParticipant(cash.getOwner());
            output2.addParticipant(stock.getOwner());

            // Step 3. Create a new TransactionBuilder object.
            final TransactionBuilder builder = new TransactionBuilder(notary);

            // Step 4. Add the Cash as an output state, as well as a command to the
            // transaction builder.
            builder.addInputState(input1);
            builder.addInputState(input2);
            builder.addOutputState(output1);
            builder.addOutputState(output2);
            builder.addCommand(new AssetContract.Commands.Transfer(),
                    Arrays.asList(this.party.getOwningKey(), this.counterparty.getOwningKey()));

            // Step 5. Verify and sign it with our KeyPair.
            builder.verify(getServiceHub());
            final SignedTransaction ptx = getServiceHub().signInitialTransaction(builder);

            // Step 6. Collect the other party's signature using the SignTransactionFlow.
            List<FlowSession> sessions = Stream.of(party, counterparty)
                    .filter(el -> !el.equals(getOurIdentity()))
                    .map(this::initiateFlow)
                    .collect(Collectors.toList());

            SignedTransaction stx = subFlow(new CollectSignaturesFlow(ptx, sessions));

            // Step 7. Assuming no exceptions, we can now finalise the transaction
            return subFlow(new FinalityFlow(stx, sessions));
        }

        private StateAndRef<Cash> queryCash(Cash c) {
            return getServiceHub().getVaultService().queryBy(Cash.class).getStates().stream()
                    .filter(it -> {
                        Cash state = it.getState().getData();
                        return state.getCurrency().equals(c.getCurrency())
                                && Math.abs(state.getAmount() - c.getAmount()) < 0.001
                                && state.getOwner().equals(c.getOwner());
                    })
                    .findFirst().orElseThrow(() -> new RuntimeException("Could not find cash state in vault"));
        }

        private StateAndRef<Stock> queryStock(Stock s) {
            return getServiceHub().getVaultService().queryBy(Stock.class).getStates().stream()
                    .filter(it -> {
                        Stock state = it.getState().getData();
                        return state.getTicker().equals(s.getTicker())
                                && Math.abs(state.getAmount() - s.getAmount()) < 0.001
                                && state.getOwner().equals(s.getOwner());
                    })
                    .findFirst().orElseThrow(() -> new RuntimeException("Could not find cash state in vault"));
        }
    }

    // ******************
    // * Responder flow *
    // ******************
    @InitiatedBy(Initiator.class)
    public static class Responder extends FlowLogic<SignedTransaction> {

        private final FlowSession otherPartySession;

        public Responder(FlowSession otherPartySession) {
            this.otherPartySession = otherPartySession;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            class SignTxFlow extends SignTransactionFlow {
                private SignTxFlow(FlowSession otherPartyFlow, ProgressTracker progressTracker) {
                    super(otherPartyFlow, progressTracker);
                }

                @Override
                protected void checkTransaction(SignedTransaction stx) {}
            }
            final SignTxFlow signTxFlow = new SignTxFlow(otherPartySession, SignTransactionFlow.Companion.tracker());
            final SecureHash txId = subFlow(signTxFlow).getId();

            return subFlow(new ReceiveFinalityFlow(otherPartySession, txId));
        }
    }
}
