import { useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import axios from 'axios';
import Button from '../components/ui/Button';
import toast from 'react-hot-toast';

interface LoginCredentials {
  email: string;
  password: string;
}

interface LoginResponse {
  token: string;
  email: string;
  expiresIn: number;
}

export default function Login() {
  const [credentials, setCredentials] = useState<LoginCredentials>({
    email: '',
    password: '',
  });
  const [isRegisterMode, setIsRegisterMode] = useState(false);

  const loginMutation = useMutation({
    mutationFn: async (creds: LoginCredentials) => {
      const endpoint = isRegisterMode ? '/api/auth/register' : '/api/auth/login';
      const response = await axios.post<LoginResponse>(
        `http://localhost:8080${endpoint}`,
        creds
      );
      return response.data;
    },
    onSuccess: (data) => {
      localStorage.setItem('itr_erp_token', data.token);
      localStorage.setItem('itr_erp_user', JSON.stringify({ username: data.email, role: 'Admin' }));
      toast.success(isRegisterMode ? 'Registration successful' : 'Login successful');
      window.location.reload();
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || (isRegisterMode ? 'Registration failed' : 'Login failed'));
    },
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    loginMutation.mutate(credentials);
  };

  return (
    <div className="min-h-screen bg-navy flex items-center justify-center p-4">
      <div className="w-full max-w-md">
        <div className="bg-bg-card rounded-[12px] shadow-lg p-8">
          <div className="text-center mb-8">
            <h1 className="font-serif text-[32px] font-bold text-navy mb-2">ITR Filing ERP</h1>
            <p className="text-[14px] text-text-muted">{isRegisterMode ? 'Create your account' : 'Sign in to your account'}</p>
          </div>

          <form onSubmit={handleSubmit} className="space-y-5">
            <div>
              <label className="block text-[13px] font-semibold text-text-secondary mb-2">
                Email
              </label>
              <input
                type="email"
                value={credentials.email}
                onChange={(e) => setCredentials(prev => ({ ...prev, email: e.target.value }))}
                className="w-full px-4 py-3 border border-border-strong rounded-lg text-[14px] outline-none focus:border-gold transition-colors"
                placeholder="Enter your email"
                required
              />
            </div>

            <div>
              <label className="block text-[13px] font-semibold text-text-secondary mb-2">
                Password
              </label>
              <input
                type="password"
                value={credentials.password}
                onChange={(e) => setCredentials(prev => ({ ...prev, password: e.target.value }))}
                className="w-full px-4 py-3 border border-border-strong rounded-lg text-[14px] outline-none focus:border-gold transition-colors"
                placeholder="Enter your password"
                required
              />
            </div>

            <Button
              type="submit"
              variant="primary"
              className="w-full py-3"
              disabled={loginMutation.isPending}
            >
              {loginMutation.isPending ? (isRegisterMode ? 'Creating account...' : 'Signing in...') : (isRegisterMode ? 'Create Account' : 'Sign In')}
            </Button>
          </form>

          <div className="mt-4 text-center">
            <button
              onClick={() => setIsRegisterMode(!isRegisterMode)}
              className="text-[13px] text-gold hover:text-gold-light transition-colors"
            >
              {isRegisterMode ? 'Already have an account? Sign in' : "Don't have an account? Register"}
            </button>
          </div>

          {!isRegisterMode && (
            <div className="mt-6 p-4 bg-gold-pale border border-gold/20 rounded-lg">
              <p className="text-[12px] text-text-secondary">
                <strong>First time?</strong> Click "Register" above to create an account with any email and password (min 8 characters).
              </p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
