# Azure Cosmos DB Containers Setup Guide

## Required Containers

You need to create the following containers in your Azure Cosmos DB database `TeamWorkChat`:

### 1. **users** ✅ (Already exists)
- **Container Name:** `users`
- **Partition Key:** `/username`
- **Purpose:** Store user accounts and authentication information

### 2. **teams** ⚠️ (Needs to be created)
- **Container Name:** `teams`
- **Partition Key:** `/name`
- **Purpose:** Store team information and members

### 3. **messages** ⚠️ (Needs to be created)
- **Container Name:** `messages`
- **Partition Key:** `/teamId`
- **Purpose:** Store team chat messages

### 4. **conversations** ⚠️ (Needs to be created)
- **Container Name:** `conversations`
- **Partition Key:** `/userId`
- **Purpose:** Store conversation summaries/views for each user

### 5. **privateMessages** ⚠️ (Needs to be created)
- **Container Name:** `privateMessages`
- **Partition Key:** `/senderId`
- **Purpose:** Store private messages between users

---

## How to Create Containers in Azure Portal

1. Go to [Azure Portal](https://portal.azure.com)
2. Navigate to your Cosmos DB account: **teamwork-chat-cosmos-db**
3. Select **Data Explorer** from the left menu
4. Click on your database: **TeamWorkChat**
5. Click **"New Container"** button
6. For each container above:
   - **Container ID:** Enter the container name (e.g., `teams`)
   - **Partition key:** Enter the partition key path (e.g., `/name`)
   - **Throughput:** Use **Manual** or **Autoscale** (minimum 400 RU/s recommended)
   - Click **OK** to create

---

## Quick Setup Commands (Optional - Using Azure CLI)

If you have Azure CLI installed, you can create all containers at once:

```bash
# Login to Azure
az login

# Set variables
RESOURCE_GROUP="your-resource-group-name"
ACCOUNT_NAME="teamwork-chat-cosmos-db"
DATABASE_NAME="TeamWorkChat"

# Create teams container
az cosmosdb sql container create \
  --account-name $ACCOUNT_NAME \
  --database-name $DATABASE_NAME \
  --name teams \
  --partition-key-path "/name" \
  --throughput 400

# Create messages container
az cosmosdb sql container create \
  --account-name $ACCOUNT_NAME \
  --database-name $DATABASE_NAME \
  --name messages \
  --partition-key-path "/teamId" \
  --throughput 400

# Create conversations container
az cosmosdb sql container create \
  --account-name $ACCOUNT_NAME \
  --database-name $DATABASE_NAME \
  --name conversations \
  --partition-key-path "/userId" \
  --throughput 400

# Create privateMessages container
az cosmosdb sql container create \
  --account-name $ACCOUNT_NAME \
  --database-name $DATABASE_NAME \
  --name privateMessages \
  --partition-key-path "/senderId" \
  --throughput 400
```

---

## Verification

After creating all containers, verify they exist in the Azure Portal:

1. Go to **Data Explorer**
2. Expand **TeamWorkChat** database
3. You should see 5 containers:
   - ✅ users
   - ✅ teams
   - ✅ messages
   - ✅ conversations
   - ✅ privateMessages

---

## Testing After Setup

Once all containers are created:

1. Restart your Spring Boot application
2. Try logging in again
3. Test creating a conversation
4. Test sending messages

All 400 errors related to "Resource Not Found" should be resolved.
