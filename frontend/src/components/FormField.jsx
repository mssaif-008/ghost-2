import React from 'react';
import './FormField.css';

export const FormField = ({ label, id, error, hint, ...props }) => {
  return (
    <div className="form-field">
      {label && (
        <label htmlFor={id} className="form-field__label">
          {label}
        </label>
      )}
      <input id={id} className="form-field__input" {...props} />
      {hint && !error && <span className="form-field__hint">{hint}</span>}
      {error && <span className="form-field__error">{error}</span>}
    </div>
  );
};
