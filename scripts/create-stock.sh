#!/bin/bash
set -x

ticker=${1:-MSFT}
amount=${2:-200.00}
partyName=${3:-O=PartyA,L=London,C=GB}
curl -d "ticker=${ticker}&amount=${amount}&partyName=${partyName}" localhost:10050/create-stock
