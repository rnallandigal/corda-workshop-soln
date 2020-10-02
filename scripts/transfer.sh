#!/bin/bash
set -x

currency=${1:-USD}
cashAmount=${2:-100.00}
cashOwnerName=${3:-O=PartyB,L=New York,C=US}
ticker=${4:-MSFT}
stockAmount=${5:-200.00}
stockOwnerName=${6:-O=PartyA,L=London,C=GB}

curl -d "currency=${currency}&cashAmount=${cashAmount}&cashOwnerName=${cashOwnerName}&ticker=${ticker}&stockAmount=${stockAmount}&stockOwnerName=${stockOwnerName}" localhost:10050/transfer
