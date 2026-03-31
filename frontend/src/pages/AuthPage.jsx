import React, { useState, useEffect } from 'react';
import { useLocation, useNavigate, Link } from 'react-router-dom';
import { FormField } from '../components/FormField';
import { Button } from '../components/Button';
import { login, signup } from '../api/auth';
import './AuthPage.css';

export const AuthPage = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const isLogin = location.pathname === '/login';
  
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    // Check if they are already logged in
    if (localStorage.getItem('ghost_token')) {
      navigate('/dashboard', { replace: true });
    }
  }, [navigate]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);
    
    if (!isLogin && password !== confirmPassword) {
      setError('Passwords do not match');
      return;
    }
    
    setLoading(true);
    try {
      let res;
      if (isLogin) {
        res = await login(email, password);
      } else {
        res = await signup(email, password);
      }
      localStorage.setItem('ghost_token', res.token);
      localStorage.setItem('ghost_email', res.email);
      navigate('/dashboard');
    } catch (err) {
      setError(err.response?.data?.message || 'Authentication failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-page page-enter">
      <div className="auth-page__left">
        <h2 className="auth-page__quote">
          "The ghost in the machine is yours."
        </h2>
      </div>
      <div className="auth-page__right">
        <div className="auth-page__tabs">
          <Link to="/login" className={`auth-page__tab ${isLogin ? 'auth-page__tab--active' : ''}`}>Sign In</Link>
          <Link to="/signup" className={`auth-page__tab ${!isLogin ? 'auth-page__tab--active' : ''}`}>Create Account</Link>
        </div>
        
        <form className="auth-page__form" onSubmit={handleSubmit}>
          {loading && (
            <div className="auth-page__authenticating">
              {'>'} Authenticating...<span className="terminal-cursor">▋</span>
            </div>
          )}
          
          <FormField 
            label="Email" 
            id="email" 
            type="email" 
            value={email} 
            onChange={e => setEmail(e.target.value)} 
            required 
            disabled={loading}
          />
          
          <FormField 
            label="Password" 
            id="password" 
            type="password" 
            value={password} 
            onChange={e => setPassword(e.target.value)} 
            required
            minLength={8}
            disabled={loading}
          />
          
          {!isLogin && (
            <FormField 
              label="Confirm Password" 
              id="confirmPassword" 
              type="password" 
              value={confirmPassword} 
              onChange={e => setConfirmPassword(e.target.value)} 
              required
              minLength={8}
              disabled={loading}
            />
          )}

          {error && <div className="auth-page__error">{error}</div>}
          
          <Button type="submit" loading={loading} style={{ width: '100%', marginTop: '1rem' }}>
            {isLogin ? 'Sign In' : 'Create Account'}
          </Button>
        </form>
      </div>
    </div>
  );
};
