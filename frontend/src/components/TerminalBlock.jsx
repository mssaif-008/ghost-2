import React from 'react';
import './TerminalBlock.css';
import { BuildStepLog } from './BuildStepLog';

export const TerminalBlock = ({ error, children, isBuilding }) => {
  if (error) {
    return (
      <div className="terminal-block terminal-block--error">
        <div className="terminal-block__header">── ERROR ──────────────────────────────────────</div>
        <pre className="terminal-block__output">{error}</pre>
      </div>
    );
  }

  return (
    <div className="terminal-block">
      {children}
      {isBuilding && <div className="terminal-block__cursor">▋</div>}
    </div>
  );
};
