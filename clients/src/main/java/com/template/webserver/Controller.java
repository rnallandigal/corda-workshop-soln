package com.template.webserver;

import com.dtcc.tril.workshop.flows.IssueCashFlow;
import com.dtcc.tril.workshop.flows.IssueStockFlow;
import com.dtcc.tril.workshop.flows.TransferFlow;
import com.dtcc.tril.workshop.states.Cash;
import com.dtcc.tril.workshop.states.Stock;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.corda.client.jackson.JacksonSupport;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.transactions.SignedTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;

/**
 * Define your API endpoints here.
 */
@RestController
@RequestMapping("/") // The paths for HTTP requests are relative to this base path.
public class Controller {
    private static final Logger logger = LoggerFactory.getLogger(RestController.class);
    private final CordaRPCOps proxy;

    public Controller(NodeRPCConnection rpc) {
        this.proxy = rpc.proxy;
    }

    @Configuration
    class Plugin {
        @Bean
        public ObjectMapper registerModule() {
            return JacksonSupport.createNonRpcMapper();
        }
    }

    @GetMapping(value = "/cash", produces = APPLICATION_JSON_VALUE)
    public List<StateAndRef<Cash>> getCash() {
        // Filter by state type: Cash.
        return proxy.vaultQuery(Cash.class).getStates();
    }

    /**
     * Displays all cash states owned by this node
     */
    @GetMapping(value = "/my-cash", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<List<StateAndRef<Cash>>> getMyCash() {
        Party me = proxy.nodeInfo().getLegalIdentities().get(0);
        List<StateAndRef<Cash>> myCash = proxy.vaultQuery(Cash.class).getStates().stream()
                .filter(it -> it.getState().getData().getOwner().equals(me))
                .collect(Collectors.toList());
        return ResponseEntity.ok(myCash);
    }

    @PostMapping(value = "/create-cash", produces = TEXT_PLAIN_VALUE)
    public ResponseEntity<String> issueCash(
            @RequestParam String currency,
            @RequestParam double amount,
            @RequestParam String partyName
    ) throws IllegalArgumentException {
        CordaX500Name partyX500Name = CordaX500Name.parse(partyName);
        Party receiver = proxy.wellKnownPartyFromX500Name(partyX500Name);

        try {
            // Start the Flow. We block and wait for the flow to return.
            SignedTransaction result = proxy.startTrackedFlowDynamic(
                    IssueCashFlow.Initiator.class,
                    currency,
                    amount,
                    receiver
            ).getReturnValue().get();

            // Return the response.
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body("Transaction id "+ result.getId() +" committed to ledger.\n " + result.getTx().getOutput(0));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping(value = "/stocks", produces = APPLICATION_JSON_VALUE)
    public List<StateAndRef<Stock>> getStocks() {
        // Filter by state type: Stock.
        return proxy.vaultQuery(Stock.class).getStates();
    }

    /**
     * Displays all stock states owned by this node
     */
    @GetMapping(value = "/my-stocks", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<List<StateAndRef<Stock>>> getMyStocks() {
        Party me = proxy.nodeInfo().getLegalIdentities().get(0);
        List<StateAndRef<Stock>> myStocks = proxy.vaultQuery(Stock.class).getStates().stream()
                .filter(it -> it.getState().getData().getOwner().equals(me))
                .collect(Collectors.toList());
        return ResponseEntity.ok(myStocks);
    }

    @PostMapping(value = "/create-stock", produces = TEXT_PLAIN_VALUE)
    public ResponseEntity<String> issueStock(
            @RequestParam String ticker,
            @RequestParam double amount,
            @RequestParam String partyName
    ) throws IllegalArgumentException {
        CordaX500Name partyX500Name = CordaX500Name.parse(partyName);
        Party receiver = proxy.wellKnownPartyFromX500Name(partyX500Name);

        try {
            // Start the Flow. We block and wait for the flow to return.
            SignedTransaction result = proxy.startTrackedFlowDynamic(
                    IssueStockFlow.Initiator.class,
                    ticker,
                    amount,
                    receiver
            ).getReturnValue().get();

            // Return the response.
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body("Transaction id "+ result.getId() +" committed to ledger.\n " + result.getTx().getOutput(0));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PostMapping(value = "/transfer", produces = TEXT_PLAIN_VALUE)
    public ResponseEntity<String> transfer(
            @RequestParam String currency,
            @RequestParam double cashAmount,
            @RequestParam String cashOwnerName,
            @RequestParam String ticker,
            @RequestParam double stockAmount,
            @RequestParam String stockOwnerName
    ) throws IllegalArgumentException {
        Party cashOwner = proxy.wellKnownPartyFromX500Name(CordaX500Name.parse(cashOwnerName));
        Party stockOwner = proxy.wellKnownPartyFromX500Name(CordaX500Name.parse(stockOwnerName));

        try {
            // Start the Flow. We block and wait for the flow to return.
            SignedTransaction result = proxy.startTrackedFlowDynamic(
                    TransferFlow.Initiator.class,
                    new Cash(currency, cashAmount, cashOwner),
                    new Stock(ticker, stockAmount, stockOwner)
            ).getReturnValue().get();

            // Return the response.
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body("Transaction id "+ result.getId() +" committed to ledger.\n " + result.getTx().getOutput(0));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}