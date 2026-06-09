export const tokenManager = {
  save(token: string, email: string, expiresIn: number) {
    localStorage.setItem('auth_token', token);
    localStorage.setItem('auth_email', email);
    localStorage.setItem('auth_expiry', String(Date.now() + expiresIn * 1000));
  },
  getToken: () => localStorage.getItem('auth_token'),
  getEmail: () => localStorage.getItem('auth_email'),
  isExpired() {
    const e = localStorage.getItem('auth_expiry');
    return !e || Date.now() > parseInt(e);
  },
  isAuthenticated() { return !!this.getToken() && !this.isExpired(); },
  clear() { localStorage.clear(); },
};
