import React, { useEffect, useState } from 'react';

export const BuildStepLog = ({ step, logOutput, startedAt, finishedAt }) => {
  const duration = finishedAt && startedAt
    ? ((new Date(finishedAt) - new Date(startedAt)) / 1000).toFixed(1)
    : '...';
    
  // Typewriter effect
  const [displayedLog, setDisplayedLog] = useState('');
  
  useEffect(() => {
    if (!logOutput) {
      setDisplayedLog('');
      return;
    }
    
    // Animate the difference if logOutput grows
    let i = displayedLog.length;
    if (i >= logOutput.length) {
      setDisplayedLog(logOutput);
      return;
    }

    const interval = setInterval(() => {
      setDisplayedLog(logOutput.substring(0, i));
      i++;
      if (i > logOutput.length) clearInterval(interval);
    }, 8); // 8ms per char

    return () => clearInterval(interval);
  }, [logOutput]);

  const headerLabel = `── ${step} `;
  const padLength = Math.max(0, 40 - headerLabel.length);
  const headerLine = headerLabel + '─'.repeat(padLength) + ` [${duration}s]`;

  return (
    <div className="build-step-log page-enter">
      <div className="build-step-log__header">{headerLine}</div>
      <pre className="build-step-log__output">{displayedLog}</pre>
    </div>
  );
};
