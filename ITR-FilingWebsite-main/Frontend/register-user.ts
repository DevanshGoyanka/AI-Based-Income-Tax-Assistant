import axios from 'axios';

// Register a test user
async function registerTestUser() {
  try {
    const response = await axios.post('http://localhost:8080/api/auth/register', {
      email: 'admin@example.com',
      password: 'password123'
    });
    console.log('User registered successfully:', response.data);
  } catch (error: any) {
    console.error('Registration failed:', error.response?.data || error.message);
  }
}

registerTestUser();
