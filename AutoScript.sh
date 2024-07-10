#!/bin/sh

# ./gradlew stopCordaAndCleanWorkspace
# ./gradlew startCorda

# ./gradlew vNodesSetup

# Get the JSON response from the API
response=$(curl -k -u admin:admin -X 'GET' 'https://localhost:8888/api/v5_2/virtualnode' -H 'accept: application/json')

# Extract the vNodes for WorldCharity, Dave, Alice, and Charlie along with their short hashes
world_charity_hash=$(echo $response | jq -r '.virtualNodes[] | select(.holdingIdentity.x500Name | contains("WorldCharity")) | .holdingIdentity.shortHash')
dave_hash=$(echo $response | jq -r '.virtualNodes[] | select(.holdingIdentity.x500Name | contains("Dave")) | .holdingIdentity.shortHash')
alice_hash=$(echo $response | jq -r '.virtualNodes[] | select(.holdingIdentity.x500Name | contains("Alice")) | .holdingIdentity.shortHash')
charlie_hash=$(echo $response | jq -r '.virtualNodes[] | select(.holdingIdentity.x500Name | contains("Charlie")) | .holdingIdentity.shortHash')

echo "WorldCharity short hash: $world_charity_hash"
echo "Dave short hash: $dave_hash"
echo "Alice short hash: $alice_hash"
echo "Charlie short hash: $charlie_hash"

# Showing Initial Charity Bank Account -------------------------------------
echo ""

# Showing Charity Bank
curl -k -u admin:admin -X 'POST' \
  "https://localhost:8888/api/v5_2/flow/$world_charity_hash" \
  -H 'accept: application/json' \
  -H 'Authorization: Basic YWRtaW46YWRtaW4=' \
  -H 'Content-Type: application/json' \
  -d '{
    "clientRequestId": "get-total-1",
    "flowClassName": "com.r3.developers.charity.workflows.CharityTotal",
    "requestBody": {
        "charity": "CN=WorldCharity, OU=Test Dept, O=R3, L=London, C=GB"
    }
}' > /dev/null 2>&1

sleep 5

response=$(curl -k -u admin:admin -X 'GET' \
  "https://localhost:8888/api/v5_2/flow/$world_charity_hash/get-total-1" \
  -H 'accept: application/json' \
  -H 'Authorization: Basic YWRtaW46YWRtaW4=')


sleep 3

# Extract the flowResult field and parse it
flow_result=$(echo $response | jq -r '.flowResult')
total_donations=$(echo $flow_result | jq -r '.totalDonations')
allocation_funds=$(echo $flow_result | jq -r '.allocationFunds')
remaining_funds=$(echo $flow_result | jq -r '."Remaining Funds"')

# Print the extracted values
echo ""
echo "Total Donations: $total_donations"
echo "Allocation Funds: $allocation_funds"
echo "Remaining Funds: $remaining_funds"

echo ""
echo ""
echo ""

echo "*Charlie has donated £123*"
echo ""
curl -k -u admin:admin -X 'POST' \
  "https://localhost:8888/api/v5_2/flow/$charlie_hash" \
  -H 'accept: application/json' \
  -H 'Authorization: Basic YWRtaW46YWRtaW4=' \
  -H 'Content-Type: application/json' \
  -d '{
    "clientRequestId": "donate-1",
    "flowClassName": "com.r3.developers.charity.workflows.DonationFlow",
    "requestBody": {
        "amount": 123,
        "holder": "CN=WorldCharity, OU=Test Dept, O=R3, L=London, C=GB"
    }
}' > /dev/null 2>&1

echo ""
echo ""

sleep 5

echo "-----Charity Bank Account-----"

curl -k -u admin:admin -X 'POST' \
  "https://localhost:8888/api/v5_2/flow/$world_charity_hash" \
  -H 'accept: application/json' \
  -H 'Authorization: Basic YWRtaW46YWRtaW4=' \
  -H 'Content-Type: application/json' \
  -d '{
    "clientRequestId": "get-total-2",
    "flowClassName": "com.r3.developers.charity.workflows.CharityTotal",
    "requestBody": {
        "charity": "CN=WorldCharity, OU=Test Dept, O=R3, L=London, C=GB"
    }
}' > /dev/null 2>&1

sleep 5

response=$(curl -k -u admin:admin -X 'GET' \
  "https://localhost:8888/api/v5_2/flow/$world_charity_hash/get-total-2" \
  -H 'accept: application/json' \
  -H 'Authorization: Basic YWRtaW46YWRtaW4=')



sleep 2

# Extract the flowResult field and parse it
flow_result=$(echo $response | jq -r '.flowResult')
total_donations=$(echo $flow_result | jq -r '.totalDonations')
allocation_funds=$(echo $flow_result | jq -r '.allocationFunds')
remaining_funds=$(echo $flow_result | jq -r '."Remaining Funds"')

# Print the extracted values
echo ""
echo "Total Donations: $total_donations"
echo "Allocation Funds: $allocation_funds"
echo "Remaining Funds: $remaining_funds"

echo ""
echo ""
echo ""
sleep 5

echo "*Charity Creates a Project called Housing*"

curl -k -u admin:admin -X 'POST' \
  "https://localhost:8888/api/v5_2/flow/$world_charity_hash" \
  -H 'accept: application/json' \
  -H 'Authorization: Basic YWRtaW46YWRtaW4=' \
  -H 'Content-Type: application/json' \
  -d '{
    "clientRequestId": "create-project-1",
    "flowClassName": "com.r3.developers.charity.workflows.ProjectCreationFlow",
    "requestBody": {
        "totalBudget": 0,
        "projectName": "Housing",
        "projectDescription": "Building homes",
        "charityName": "CN=WorldCharity, OU=Test Dept, O=R3, L=London, C=GB",
        "notary": "CN=NotaryService, OU=Test Dept, O=R3, L=London, C=GB"
    }
}'

sleep 5

curl -k -u admin:admin -X 'POST' \
  "https://localhost:8888/api/v5_2/flow/$world_charity_hash" \
  -H 'accept: application/json' \
  -H 'Authorization: Basic YWRtaW46YWRtaW4=' \
  -H 'Content-Type: application/json' \
  -d '{
    "clientRequestId": "get-ID-name",
    "flowClassName": "com.r3.developers.charity.workflows.GetProjectIDGivenProjectName",
    "requestBody": {
        "projectName": "Housing"
    }
}'
sleep 5

response=$(curl -k -u admin:admin -X 'GET' \
  "https://localhost:8888/api/v5_2/flow/$world_charity_hash/get-ID-name" \
  -H 'accept: application/json' \
  -H 'Authorization: Basic YWRtaW46YWRtaW4=')

flow_result=$(echo $response | jq -r '.flowResult')
projectID=$(echo $flow_result | jq -r '.projectID')

echo "$projectID"

sleep 8
#------------------------------------------------------------------------

# Make the POST request to initiate the flow
response=$(curl -k -u admin:admin -X 'POST' \
  "https://localhost:8888/api/v5_2/flow/$world_charity_hash/get-create-1" \
  -H 'accept: application/json' \
  -H 'Authorization: Basic YWRtaW46YWRtaW4=' \
  -H 'Content-Type: application/json' \
  -d "{
    \"clientRequestId\": \"get-create-1\",
    \"flowClassName\": \"com.r3.developers.charity.workflows.GetProjectDetailsFlow\",
    \"requestBody\": {
        \"projectID\": \"$projectID\"
    }
}")

#








