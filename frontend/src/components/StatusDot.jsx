import React from 'react';

export const StatusDot = ({ status }) => {
  let color = '#888888';
  let pulse = false;

  switch (status) {
    case 'QUEUED':
      color = '#888888';
      break;
    case 'BUILDING':
      color = '#F39C12'; // amber
      pulse = true;
      break;
    case 'UPLOADING':
      color = '#3498DB'; // blue
      pulse = true;
      break;
    case 'LIVE':
      color = '#2ECC71'; // green
      break;
    case 'FAILED':
      color = '#C0392B'; // red
      break;
    default:
      break;
  }

  const dotStyle = {
    display: 'inline-block',
    width: '10px',
    height: '10px',
    borderRadius: '50%',
    backgroundColor: color,
    marginRight: '0.75rem',
    ...(pulse ? { animation: 'pulse 1.5s infinite', '--pulse-color': color } : {}),
  };

  return <span style={dotStyle} aria-label={`Status: ${status}`} />;
};
