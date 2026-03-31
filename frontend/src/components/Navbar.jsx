import React from 'react';
import { useNavigate } from 'react-router-dom';
import { Button } from './Button';
import './Navbar.css';

export const Navbar = () => {
  const navigate = useNavigate();
  const email = localStorage.getItem('ghost_email');

  const handleLogout = () => {
    localStorage.removeItem('ghost_token');
    localStorage.removeItem('ghost_email');
    navigate('/');
  };

  return (
    <nav className="navbar">
      <div className="navbar__logo" onClick={() => navigate(email ? '/dashboard' : '/')}>
        Ghost Host
      </div>
      {email && (
        <div className="navbar__user">
          <span className="navbar__email">{email}</span>
          <Button variant="ghost" onClick={handleLogout}>Logout</Button>
        </div>
      )}
    </nav>
  );
};
