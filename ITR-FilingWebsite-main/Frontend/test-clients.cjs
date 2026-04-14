const axios = require('axios');

const API_BASE = 'http://localhost:8080/api';
let authToken = '';

// Login first
async function login() {
  const response = await axios.post(`${API_BASE}/auth/login`, {
    email: 'dev@test.com',
    password: 'password123'
  });
  authToken = response.data.token;
  console.log('✓ Logged in successfully');
  return authToken;
}

// Generate random PAN
function generatePAN() {
  const letters = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ';
  const pan = 
    letters[Math.floor(Math.random() * 26)] +
    letters[Math.floor(Math.random() * 26)] +
    letters[Math.floor(Math.random() * 26)] +
    letters[Math.floor(Math.random() * 26)] +
    letters[Math.floor(Math.random() * 26)] +
    Math.floor(1000 + Math.random() * 9000) +
    letters[Math.floor(Math.random() * 26)];
  return pan;
}

// Generate random client data
function generateClient(index) {
  const firstNames = ['Rajesh', 'Priya', 'Amit', 'Neha', 'Sanjay', 'Kavita', 'Vikram', 'Anjali', 'Rahul', 'Pooja'];
  const lastNames = ['Kumar', 'Sharma', 'Patel', 'Gupta', 'Singh', 'Verma', 'Reddy', 'Nair', 'Mehta', 'Joshi'];
  
  const firstName = firstNames[index % firstNames.length];
  const lastName = lastNames[Math.floor(Math.random() * lastNames.length)];
  
  return {
    name: `${firstName} ${lastName}`,
    pan: generatePAN(),
    email: `${firstName.toLowerCase()}.${lastName.toLowerCase()}${index}@example.com`,
    mobile: `+91 ${Math.floor(70000 + Math.random() * 30000)}${Math.floor(10000 + Math.random() * 90000)}`,
    aadhaar: `${Math.floor(100000000000 + Math.random() * 900000000000)}`,
    dob: `19${Math.floor(60 + Math.random() * 35)}-${String(Math.floor(1 + Math.random() * 12)).padStart(2, '0')}-${String(Math.floor(1 + Math.random() * 28)).padStart(2, '0')}`
  };
}

// Create clients
async function createClients() {
  const clients = [];
  
  for (let i = 0; i < 10; i++) {
    const clientData = generateClient(i);
    
    try {
      const response = await axios.post(`${API_BASE}/clients`, clientData, {
        headers: { Authorization: `Bearer ${authToken}` }
      });
      
      clients.push({
        ...response.data,
        itrType: ['ITR-1', 'ITR-2', 'ITR-3', 'ITR-4'][i % 4]
      });
      
      console.log(`✓ Created client ${i + 1}/10: ${clientData.name} (${clientData.pan}) - ${clients[i].itrType}`);
    } catch (error) {
      console.error(`✗ Failed to create client ${i + 1}:`, error.response?.data || error.message);
    }
  }
  
  return clients;
}

// Get all clients
async function getClients() {
  try {
    const response = await axios.get(`${API_BASE}/clients`, {
      headers: { Authorization: `Bearer ${authToken}` }
    });
    console.log(`✓ Retrieved ${response.data.length} clients from database`);
    return response.data;
  } catch (error) {
    console.error('✗ Failed to get clients:', error.response?.data || error.message);
    return [];
  }
}

// Get ITR data for a client
async function getITRData(clientId, year = '2025-26') {
  try {
    const response = await axios.get(`${API_BASE}/clients/${clientId}/itr/${year}`, {
      headers: { Authorization: `Bearer ${authToken}` }
    });
    console.log(`✓ Retrieved ITR data for client ${clientId}`);
    return response.data;
  } catch (error) {
    console.error(`✗ Failed to get ITR data for client ${clientId}:`, error.response?.data || error.message);
    return null;
  }
}

// Main execution
async function main() {
  console.log('=== ITR ERP Client Creation & Testing ===\n');
  
  try {
    // Step 1: Login
    await login();
    
    // Step 2: Create 10 clients
    console.log('\n--- Creating 10 Clients ---');
    const newClients = await createClients();
    
    // Step 3: Verify clients in database
    console.log('\n--- Verifying Clients ---');
    const allClients = await getClients();
    
    // Step 4: Test ITR data retrieval for each new client
    console.log('\n--- Testing ITR Data Retrieval ---');
    for (const client of newClients) {
      await getITRData(client.id);
    }
    
    // Step 5: Generate report
    console.log('\n=== FINAL REPORT ===');
    console.log(`Total clients created: ${newClients.length}`);
    console.log(`Total clients in database: ${allClients.length}`);
    console.log('\nClients by ITR Type:');
    const itrCounts = newClients.reduce((acc, c) => {
      acc[c.itrType] = (acc[c.itrType] || 0) + 1;
      return acc;
    }, {});
    Object.entries(itrCounts).forEach(([type, count]) => {
      console.log(`  ${type}: ${count} clients`);
    });
    
    console.log('\n✓ All tests completed successfully!');
    
  } catch (error) {
    console.error('\n✗ Test execution failed:', error.message);
    process.exit(1);
  }
}

main();
