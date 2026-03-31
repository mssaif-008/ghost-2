import React from 'react';
import './Button.css';

export const Button = ({ children, variant = 'primary', loading = false, className = '', ...props }) => {
  return (
    <button 
      className={`btn btn--${variant} ${loading ? 'btn--loading' : ''} ${className}`} 
      disabled={loading || props.disabled} 
      {...props}
    >
      <span className="btn__text">
        {loading ? '> ...' : children}
      </span>
    </button>
  );
};
