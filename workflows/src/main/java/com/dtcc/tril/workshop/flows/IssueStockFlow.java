package com.dtcc.tril.workshop.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.dtcc.tril.workshop.contracts.AssetContract;
import com.dtcc.tril.workshop.states.Stock;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static net.corda.core.contracts.ContractsDSL.requireThat;

public class IssueStockFlow {
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
        private Party sender;
        private Party receiver;
        private double amount;
        private String ticker;

        // public constructor
        public Initiator(String ticker, double amount, Party sendTo) {
            this.receiver = sendTo;
            this.amount = amount;
            this.ticker = ticker;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            this.sender = getOurIdentity();

            // Step 1. Get a reference to the notary service on our network and our key pair.
            final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

            // Compose the Stock state to be issued
            final Stock output = new Stock(ticker, amount, receiver);
            output.addParticipant(sender);

            // Step 3. Create a new TransactionBuilder object.
            final TransactionBuilder builder = new TransactionBuilder(notary);

            // Step 4. Add the Stock as an output state, as well as a command to the
            // transaction builder.
            builder.addOutputState(output);
            builder.addCommand(new AssetContract.Commands.IssueStock(),
                    Arrays.asList(this.sender.getOwningKey(), this.receiver.getOwningKey()));

            // Step 5. Verify and sign it with our KeyPair.
            builder.verify(getServiceHub());
            final SignedTransaction ptx = getServiceHub().signInitialTransaction(builder);

            // Step 6. Collect the other party's signature using the SignTransactionFlow.
            List<FlowSession> sessions = output.getParticipants().stream()
                    .map(el -> (Party) el)
                    .filter(el -> !el.equals(sender))
                    .map(this::initiateFlow)
                    .collect(Collectors.toList());

            SignedTransaction stx = subFlow(new CollectSignaturesFlow(ptx, sessions));

            // Step 7. Assuming no exceptions, we can now finalise the transaction
            return subFlow(new FinalityFlow(stx, sessions));
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
