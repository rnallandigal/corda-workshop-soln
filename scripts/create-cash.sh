#!/bin/bash
set -x

currency=${1:-USD}
amount=${2:-100.00}
partyName=${3:-O=PartyB,L=New York,C=US}
curl -d "currency=${currency}&amount=${amount}&partyName=${partyName}" localhost:10050/create-cash
